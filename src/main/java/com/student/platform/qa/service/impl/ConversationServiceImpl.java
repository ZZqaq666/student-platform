package com.student.platform.qa.service.impl;

import com.student.platform.qa.entity.Conversation;
import com.student.platform.qa.entity.Message;
import com.student.platform.qa.mapper.ConversationMapper;
import com.student.platform.qa.mapper.MessageMapper;
import com.student.platform.qa.service.ConversationService;
import com.student.platform.qa.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final SessionService sessionService;

    /**
     * 会话标题最大长度
     */
    private static final int MAX_TITLE_LENGTH = 20;

    /**
     * 消息预览最大长度
     */
    private static final int MAX_PREVIEW_LENGTH = 50;

    @Override
    @Transactional
    public Conversation createConversation(Long userId, String firstMessage) {
        // 生成会话ID
        String sessionId = sessionService.createSession();
        
        // 生成标题（取第一条消息前20个字符）
        String title = generateTitle(firstMessage);
        
        // 创建会话实体
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .sessionId(sessionId)
                .title(title)
                .lastMessagePreview(generatePreview(firstMessage))
                .messageCount(0)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // 保存会话
        conversationMapper.insert(conversation);
        
        log.info("创建会话成功，用户ID: {}, 会话ID: {}, 标题: {}", userId, sessionId, title);
        return conversation;
    }

    @Override
    public Conversation getConversationBySessionId(String sessionId) {
        return conversationMapper.selectBySessionId(sessionId);
    }

    @Override
    public Conversation getConversationBySessionIdAndUserId(String sessionId, Long userId) {
        return conversationMapper.selectBySessionIdAndUserId(sessionId, userId);
    }

    @Override
    public List<Conversation> getUserConversations(Long userId) {
        return conversationMapper.selectActiveConversationsByUserId(userId);
    }

    @Override
    @Transactional
    public void updateConversation(String sessionId, String lastMessage) {
        Conversation conversation = conversationMapper.selectBySessionId(sessionId);
        if (conversation != null) {
            // 更新最后消息预览
            conversation.setLastMessagePreview(generatePreview(lastMessage));
            // 更新消息数量
            int messageCount = messageMapper.countBySessionId(sessionId);
            conversation.setMessageCount(messageCount);
            // 更新时间
            conversation.setUpdatedAt(LocalDateTime.now());
            
            conversationMapper.updateById(conversation);
            log.debug("更新会话成功，会话ID: {}", sessionId);
        }
    }

    @Override
    @Transactional
    public void deleteConversation(String sessionId, Long userId) {
        // 权限验证
        Conversation conversation = conversationMapper.selectBySessionIdAndUserId(sessionId, userId);
        if (conversation == null) {
            log.warn("删除会话失败：会话不存在或无权限，会话ID: {}, 用户ID: {}", sessionId, userId);
            return;
        }
        
        // 逻辑删除会话
        conversation.setStatus("DELETED");
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        
        // 删除会话的所有消息（物理删除）
        messageMapper.deleteBySessionId(sessionId);
        
        // 使会话失效
        sessionService.invalidateSession(sessionId);
        
        log.info("删除会话成功，会话ID: {}, 用户ID: {}", sessionId, userId);
    }

    @Override
    public boolean isConversationOwner(String sessionId, Long userId) {
        Conversation conversation = conversationMapper.selectBySessionIdAndUserId(sessionId, userId);
        return conversation != null;
    }

    @Override
    public int getMessageCount(String sessionId) {
        return messageMapper.countBySessionId(sessionId);
    }

    /**
     * 生成会话标题
     */
    private String generateTitle(String message) {
        if (message == null || message.isEmpty()) {
            return "新会话";
        }
        // 去除首尾空白
        String trimmed = message.trim();
        // 如果超过最大长度，截取并添加省略号
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            return trimmed.substring(0, MAX_TITLE_LENGTH) + "...";
        }
        return trimmed;
    }

    /**
     * 生成消息预览
     */
    private String generatePreview(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        String trimmed = message.trim();
        if (trimmed.length() > MAX_PREVIEW_LENGTH) {
            return trimmed.substring(0, MAX_PREVIEW_LENGTH) + "...";
        }
        return trimmed;
    }
}