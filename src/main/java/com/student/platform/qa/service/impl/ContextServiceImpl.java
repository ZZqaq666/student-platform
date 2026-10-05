package com.student.platform.qa.service.impl;
import java.time.ZoneId;
import com.student.platform.qa.entity.Message;
import com.student.platform.qa.service.ContextService;
import com.student.platform.qa.service.MessageService;
import com.student.platform.qa.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文处理服务实现
 * 实现滑动窗口策略、上下文压缩和向量检索增强（RAG）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextServiceImpl implements ContextService {

    private final VectorService vectorService;
    private final MessageService messageService;

    /**
     * 存储会话消息：sessionId -> List<ContextMessage>（内存缓存）
     */
    private final Map<String, List<ContextMessage>> messageStore = new ConcurrentHashMap<>();

    /**
     * 是否已从数据库加载过该会话的消息
     */
    private final Set<String> loadedSessions = ConcurrentHashMap.newKeySet();

    /**
     * 最大上下文窗口大小（消息数量）
     */
    @Value("${context.max-messages:50}")
    private int maxMessages;

    /**
     * 滑动窗口大小（保留最近的消息数量）
     */
    @Value("${context.sliding-window-size:20}")
    private int slidingWindowSize;

    /**
     * RAG检索的相关消息数量
     */
    @Value("${context.rag-top-k:5}")
    private int ragTopK;

    /**
     * 相似度阈值（低于此阈值的消息不纳入上下文）
     */
    @Value("${context.similarity-threshold:0.3}")
    private double similarityThreshold;

    /**
     * 启用RAG检索
     */
    @Value("${context.enable-rag:true}")
    private boolean enableRag;

    /**
     * 估计的token/字符比率
     */
    private static final double TOKENS_PER_CHAR = 0.75;

    @Override
    public String getContext(String sessionId, String currentQuestion) {
        // 确保从数据库加载历史消息（只加载一次）
        loadHistoryFromDatabase(sessionId);

        List<ContextMessage> history = messageStore.getOrDefault(sessionId, Collections.emptyList());

        if (history.isEmpty()) {
            // 没有历史记录，只返回当前问题
            return buildPrompt(new ArrayList<>(), currentQuestion);
        }

        // 策略1：如果消息数量不多，直接使用所有历史
        if (history.size() <= slidingWindowSize) {
            return buildPrompt(history, currentQuestion);
        }

        // 策略2：混合使用滑动窗口和RAG检索
        List<ContextMessage> contextMessages = new ArrayList<>();

        // 添加最近的消息（滑动窗口）
        int startIndex = Math.max(0, history.size() - slidingWindowSize);
        List<ContextMessage> recentMessages = history.subList(startIndex, history.size());
        contextMessages.addAll(recentMessages);

        // 如果启用了RAG，从更早的历史中检索相关消息
        if (enableRag && startIndex > 0) {
            List<ContextMessage> earlierMessages = history.subList(0, startIndex);
            List<ContextMessage> relevantMessages = retrieveRelevantMessages(sessionId, earlierMessages,
                    currentQuestion);
            // 去重并保持顺序
            Set<String> existingIds = new HashSet<>();
            for (ContextMessage msg : contextMessages) {
                existingIds.add(msg.messageId());
            }
            for (ContextMessage msg : relevantMessages) {
                if (!existingIds.contains(msg.messageId())) {
                    contextMessages.add(msg);
                    existingIds.add(msg.messageId());
                }
            }
        }

        // 按时间顺序排序
        contextMessages.sort(Comparator.comparingLong(ContextMessage::timestamp));

        return buildPrompt(contextMessages, currentQuestion);
    }

    /**
     * 从数据库加载历史消息到内存
     */
    private void loadHistoryFromDatabase(String sessionId) {
        // 如果已经加载过，直接返回
        if (loadedSessions.contains(sessionId)) {
            return;
        }

        try {
            // 从数据库获取该会话的所有消息
            List<Message> dbMessages = messageService.getMessagesBySessionId(sessionId);

            if (dbMessages != null && !dbMessages.isEmpty()) {
                List<ContextMessage> contextMessages = new ArrayList<>();

                for (Message msg : dbMessages) {
                    ContextMessage contextMsg = new ContextMessage(
                            msg.getMessageId(),
                            msg.getRole(),
                            msg.getContent(),
                            msg.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                    contextMessages.add(contextMsg);
                }

                // 按时间顺序排序
                contextMessages.sort(Comparator.comparingLong(ContextMessage::timestamp));

                // 更新内存缓存
                messageStore.put(sessionId, contextMessages);
                log.info("从数据库加载历史消息成功，会话ID: {}, 消息数量: {}", sessionId, contextMessages.size());
            }

            // 标记为已加载
            loadedSessions.add(sessionId);
        } catch (Exception e) {
            log.error("从数据库加载历史消息失败，会话ID: {}, 错误: {}", sessionId, e.getMessage());
            // 标记为已尝试加载，避免重复尝试
            loadedSessions.add(sessionId);
        }
    }

    @Override
    public void saveMessage(String sessionId, String messageId, String role, String content) {
        ContextMessage message = new ContextMessage(messageId, role, content, System.currentTimeMillis());

        messageStore.compute(sessionId, (key, messages) -> {
            if (messages == null) {
                messages = new ArrayList<>();
            }

            // 检查是否已存在相同messageId的消息
            messages.removeIf(m -> m.messageId().equals(messageId));
            messages.add(message);

            // 如果超过最大消息数，移除最早的消息
            while (messages.size() > maxMessages) {
                ContextMessage removed = messages.remove(0);
                log.debug("消息超出最大限制，移除最早消息: {}", removed.messageId());
            }

            return messages;
        });

        // 将消息向量化存储（用于RAG检索）
        try {
            vectorService.storeVector(sessionId, messageId, content, role);
        } catch (Exception e) {
            log.warn("存储向量失败: {}", e.getMessage());
        }

        log.debug("消息已保存，会话ID: {}, 角色: {}, 消息ID: {}", sessionId, role, messageId);
    }

    @Override
    public List<ContextMessage> getHistoryMessages(String sessionId) {
        return messageStore.getOrDefault(sessionId, Collections.emptyList());
    }

    @Override
    public void clearContext(String sessionId) {
        messageStore.remove(sessionId);
        vectorService.deleteVectors(sessionId);
        log.debug("会话上下文已清空，会话ID: {}", sessionId);
    }

    @Override
    public int getContextTokenCount(String sessionId) {
        List<ContextMessage> messages = messageStore.getOrDefault(sessionId, Collections.emptyList());
        int totalChars = messages.stream()
                .mapToInt(m -> m.content().length())
                .sum();
        return (int) (totalChars * TOKENS_PER_CHAR);
    }

    /**
     * 使用RAG检索相关消息
     */
    private List<ContextMessage> retrieveRelevantMessages(String sessionId, List<ContextMessage> messages,
            String query) {
        try {
            List<VectorService.VectorSearchResult> results = vectorService.searchSimilar(sessionId, query, ragTopK);

            List<ContextMessage> relevantMessages = new ArrayList<>();
            for (VectorService.VectorSearchResult result : results) {
                // 过滤低相似度的结果
                if (result.similarity() >= similarityThreshold) {
                    // 找到对应的消息
                    for (ContextMessage msg : messages) {
                        if (msg.messageId().equals(result.messageId())) {
                            relevantMessages.add(msg);
                            break;
                        }
                    }
                }
            }

            log.debug("RAG检索完成，会话ID: {}, 检索到相关消息数: {}", sessionId, relevantMessages.size());
            return relevantMessages;
        } catch (Exception e) {
            log.warn("RAG检索失败，降级为不使用RAG: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 构建大模型的输入prompt
     */
    private String buildPrompt(List<ContextMessage> history, String currentQuestion) {
        StringBuilder prompt = new StringBuilder();

        // 添加历史对话
        for (ContextMessage msg : history) {
            if ("user".equals(msg.role())) {
                prompt.append("用户: ").append(msg.content()).append("\n");
            } else if ("assistant".equals(msg.role())) {
                prompt.append("助手: ").append(msg.content()).append("\n");
            }
        }

        // 添加当前问题
        prompt.append("用户: ").append(currentQuestion).append("\n");
        prompt.append("助手: ");

        return prompt.toString();
    }
}