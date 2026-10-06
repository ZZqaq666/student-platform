package com.student.platform.qa.service.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.common.dto.ExamPaper;
import com.student.platform.common.dto.ExamPaperResponse;
import com.student.platform.common.dto.Question;
import com.student.platform.common.dto.QaResponse;
import com.student.platform.common.dto.QaSessionDTO;
import com.student.platform.qa.entity.*;
import com.student.platform.qa.mapper.*;
import com.student.platform.qa.service.AiService;
import com.student.platform.qa.service.QaService;
import com.student.platform.qa.service.util.BilibiliVideoSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QaServiceImpl implements QaService {
    
    private static final ObjectMapper mapper = new ObjectMapper();

    private final QaHistoryMapper qaHistoryMapper;
    private final ExamQaHistoryMapper examQaHistoryMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AiService aiService;
    private final BilibiliVideoSearchService bilibiliVideoSearchService;

    @Override
    public List<QaResponse> getHistory(Long userId) {
        try {
            if (userId == null) {
                log.warn("userId为null，返回空历史记录");
                return new ArrayList<>();
            }
            
            log.info("用户ID {} 查询历史记录", userId);
            
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.error("用户ID {} 不存在", userId);
                return new ArrayList<>();
            }

            List<QaHistory> histories = new ArrayList<>();
            
            // 总是从数据库获取最新数据，确保数据一致性
            histories = qaHistoryMapper.findByUserId(user.getId());
            log.info("从数据库获取到 {} 条历史记录", histories != null ? histories.size() : 0);
            
            try {
                // 根据数据库结果更新Redis缓存
                String userHistoryListKey = "qa:user:history:" + user.getId();
                
                if (histories != null && !histories.isEmpty()) {
                    // 先清空Redis中的历史记录列表，避免缓存脏数据
                    redisTemplate.delete(userHistoryListKey);
                    
                    for (QaHistory history : histories) {
                        String historyKey = "qa:history:" + user.getId() + ":" + history.getId();
                        String processedTextKey = "qa:processed:" + user.getId() + ":" + history.getId();
                        
                        redisTemplate.opsForValue().set(historyKey, history, java.time.Duration.ofHours(24));
                        redisTemplate.opsForValue().set(processedTextKey, history.getAnswer(), java.time.Duration.ofHours(24));
                        redisTemplate.opsForList().leftPush(userHistoryListKey, history.getId());
                    }
                    
                    // 限制用户历史记录列表长度
                    redisTemplate.opsForList().trim(userHistoryListKey, 0, 99);
                    
                    log.info("历史记录已缓存到Redis");
                } else {
                    // 数据库为空时，清空Redis缓存
                    // 从Redis中获取所有历史记录ID
                    List<Object> historyIds = redisTemplate.opsForList().range(userHistoryListKey, 0, -1);
                    // 删除所有历史记录缓存
                    for (Object id : historyIds) {
                        String historyKey = "qa:history:" + user.getId() + ":" + id;
                        String processedTextKey = "qa:processed:" + user.getId() + ":" + id;
                        redisTemplate.delete(historyKey);
                        redisTemplate.delete(processedTextKey);
                    }
                    // 删除历史记录列表
                    redisTemplate.delete(userHistoryListKey);
                    
                    log.info("Redis缓存已清空，因为数据库中没有历史记录");
                }
            } catch (Exception e) {
                log.error("更新Redis缓存失败: {}", e.getMessage());
                // Redis操作失败不影响返回结果
            }
            
            // 按创建时间降序排序（最新的在前）
            List<QaHistory> historyList = histories != null ? histories : new ArrayList<>();
            return historyList.stream()
                    .sorted(Comparator.comparing(QaHistory::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取历史记录失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    @Override
    public List<QaSessionDTO> getHistorySessions(Long userId) {
        try {
            if (userId == null) {
                log.warn("userId为null，返回空会话列表");
                return new ArrayList<>();
            }
            
            log.info("用户ID {} 查询历史会话", userId);
            
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.error("用户ID {} 不存在", userId);
                return new ArrayList<>();
            }

            List<QaHistory> histories = qaHistoryMapper.findByUserId(user.getId());
            log.info("从数据库获取到 {} 条历史记录", histories != null ? histories.size() : 0);
            
            if (histories == null || histories.isEmpty()) {
                return new ArrayList<>();
            }

            Map<String, QaSessionDTO> sessionMap = new LinkedHashMap<>();
            
            for (QaHistory history : histories) {
                String sessionId = history.getSessionId();
                if (sessionId == null || sessionId.isEmpty()) {
                    sessionId = "single_" + history.getId();
                }
                
                QaSessionDTO session = sessionMap.computeIfAbsent(sessionId, k -> {
                    QaSessionDTO s = new QaSessionDTO();
                    s.setSessionId(k);
                    s.setBookId(history.getBookId());
                    s.setContext(history.getContext());
                    s.setCreatedAt(history.getCreatedAt());
                    s.setUpdatedAt(history.getCreatedAt());
                    s.setMessages(new ArrayList<>());
                    return s;
                });
                
                QaSessionDTO.QaMessageDTO message = new QaSessionDTO.QaMessageDTO();
                message.setId(history.getId());
                message.setQuestion(history.getQuestion());
                message.setAnswer(history.getAnswer());
                message.setCreatedAt(history.getCreatedAt());
                session.getMessages().add(message);
                
                if (session.getTitle() == null || session.getTitle().isEmpty()) {
                    String title = history.getQuestion();
                    if (title.length() > 40) {
                        title = title.substring(0, 40) + "...";
                    }
                    session.setTitle(title);
                }
                
                if (history.getCreatedAt().isAfter(session.getUpdatedAt())) {
                    session.setUpdatedAt(history.getCreatedAt());
                }
            }
            
            List<QaSessionDTO> sessionList = new ArrayList<>(sessionMap.values());
            
            // 对每个会话内的消息按创建时间正序排序（最早的在前）
            for (QaSessionDTO session : sessionList) {
                session.getMessages().sort(Comparator.comparing(QaSessionDTO.QaMessageDTO::getCreatedAt));
            }
            
            // 会话列表按更新时间降序排序（最新的在前）
            sessionList.sort(Comparator.comparing(QaSessionDTO::getUpdatedAt).reversed());
            
            return sessionList;
            
        } catch (Exception e) {
            log.error("获取历史会话失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    public void deleteHistory(Long id) {
        try {
            log.info("删除历史记录，ID: {}", id);
            
            // 先获取历史记录，以获取userId
            QaHistory history = qaHistoryMapper.selectById(id);
            if (history != null) {
                Long userId = history.getUserId();
                
                // 从Redis中删除缓存
                try {
                    String userHistoryListKey = "qa:user:history:" + userId;
                    String historyKey = "qa:history:" + userId + ":" + id;
                    String processedTextKey = "qa:processed:" + userId + ":" + id;
                    
                    // 从用户历史记录列表中移除该记录ID
                    redisTemplate.opsForList().remove(userHistoryListKey, 0, id);
                    // 删除历史记录缓存
                    redisTemplate.delete(historyKey);
                    // 删除处理后的文本缓存
                    redisTemplate.delete(processedTextKey);
                    
                    log.info("Redis缓存删除成功，历史记录ID: {}", id);
                } catch (Exception e) {
                    log.error("删除Redis缓存失败: {}", e.getMessage());
                    // Redis操作失败不影响主流程
                }
            }
            
            // 从数据库中删除记录
            qaHistoryMapper.deleteById(id);
            log.info("历史记录删除成功，ID: {}", id);
        } catch (Exception e) {
            log.error("删除历史记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除历史记录失败，请稍后重试");
        }
    }
    private QaResponse convertToResponse(QaHistory history) {
        return new QaResponse(
                history.getId(),
                history.getSessionId(),
                history.getQuestion(),
                history.getAnswer(),
                history.getContext(),
                history.getBookId(),
                history.getKnowledgeNodeId(),
                history.getCreatedAt()
        );
    }
    private String getRecentQaContext() {
        try {
            // 从数据库获取最近的问答记录
            List<QaHistory> recentHistory = qaHistoryMapper.findRecentByLimit(5);
            if (recentHistory != null && !recentHistory.isEmpty()) {
                StringBuilder context = new StringBuilder();
                context.append("用户最近的学习问题和回答：\n");
                for (QaHistory history : recentHistory) {
                    context.append("问题：").append(history.getQuestion()).append("\n");
                    context.append("回答：").append(history.getAnswer()).append("\n\n");
                }
                return context.toString();
            }
        } catch (Exception e) {
            log.error("获取最近问答记录失败: {}", e.getMessage());
        }
        return "";
    }
    private String preprocessAiResponse(String aiResponse) {
        log.info("开始预处理AI响应，原始长度: {}", aiResponse.length());
        
        if (aiResponse.trim().isEmpty()) {
            log.warn("AI响应为空");
            return "{}";
        }
        
        String processed = aiResponse;
        
        try {
            // 1. 移除HTML标签
            processed = processed.replaceAll("<[^>]*>", "").trim();
            
            // 2. 移除开头的单引号
            if (processed.startsWith("'")) {
                processed = processed.substring(1).trim();
            }
            
            // 3. 移除结尾的单引号
            if (processed.endsWith("'")) {
                processed = processed.substring(0, processed.length() - 1).trim();
            }
            
            // 4. 移除Markdown代码块标记
            processed = processed.replaceAll("^[\\s\\n]*```[\\s\\n]*json[\\s\\n]*", "").trim();
            processed = processed.replaceAll("[\\s\\n]*```[\\s\\n]*$", "").trim();
            
            // 5. 清理多余的空白字符
            processed = processed.trim();
            
            // 6. 检查JSON是否完整
            if (isJsonComplete(processed)) {
                log.warn("JSON不完整，尝试修复...");
                processed = fixTruncatedJson(processed);
            }
            
            // 7. 处理JSON数据，移除无效字符，确保格式正确
            processed = cleanAndFixJson(processed);
            
            // 8. 再次检查JSON完整性
            if (isJsonComplete(processed)) {
                log.error("修复后JSON仍不完整，返回空对象");
                return "{}";
            }
            
            log.info("预处理完成，处理后长度: {}", processed.length());
            return processed;
            
        } catch (Exception e) {
            log.error("预处理AI响应失败: {}", e.getMessage(), e);
            return "{}";
        }
    }
    private boolean isJsonComplete(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return true;
        }
        
        String cleaned = jsonString.trim();
        
        // 检查是否以 { 或 [ 开头
        if (!cleaned.startsWith("{") && !cleaned.startsWith("[")) {
            return true;
        }
        
        // 检查括号是否匹配
        Stack<Character> stack = new Stack<>();
        boolean inString = false;
        char prevChar = 0;
        
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            
            // 处理转义字符
            if (prevChar == '\\') {
                prevChar = c;
                continue;
            }
            
            // 处理字符串内的内容
            if (c == '"') {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '{' || c == '[') {
                    stack.push(c);
                } else if (c == '}') {
                    if (stack.isEmpty() || stack.peek() != '{') {
                        return true;
                    }
                    stack.pop();
                } else if (c == ']') {
                    if (stack.isEmpty() || stack.peek() != '[') {
                        return true;
                    }
                    stack.pop();
                }
            }
            
            prevChar = c;
        }
        
        // 栈应为空且不在字符串内
        return !stack.isEmpty() || inString;
    }
    
    /**
     * 修复被截断的JSON
     */
    private String fixTruncatedJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return "{}";
        }
        
        String cleaned = jsonString.trim();
        log.debug("修复前JSON: {}", cleaned.length() > 200 ? cleaned.substring(0, 200) + "..." : cleaned);
        
        // 如果JSON被截断在字符串内，需要特殊处理
        StringBuilder fixed = new StringBuilder(cleaned);
        
        // 统计括号数量
        int openBraces = 0;
        int openBrackets = 0;
        boolean inString = false;
        char prevChar = 0;
        
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            
            if (prevChar == '\\') {
                prevChar = c;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '{') openBraces++;
                if (c == '}') openBraces--;
                if (c == '[') openBrackets++;
                if (c == ']') openBrackets--;
            }
            
            prevChar = c;
        }
        
        // 添加缺失的关闭括号
        while (openBraces > 0) {
            fixed.append("}");
            openBraces--;
        }
        
        while (openBrackets > 0) {
            fixed.append("]");
            openBrackets--;
        }
        
        // 如果还在字符串内，关闭字符串
        if (inString) {
            fixed.append("\"");
        }
        
        // 关闭所有未关闭的数组和对象
        String result = fixed.toString();
        
        // 确保以 } 或 ] 结尾
        if (!result.endsWith("}") && !result.endsWith("]")) {
            // 尝试找到最后一个有效的结束位置
            int lastBrace = result.lastIndexOf("}");
            int lastBracket = result.lastIndexOf("]");
            int lastValidEnd = Math.max(lastBrace, lastBracket);
            
            if (lastValidEnd > 0) {
                result = result.substring(0, lastValidEnd + 1);
            } else {
                // 如果没有找到，添加一个结束括号
                result = result + "}";
            }
        }
        
        log.debug("修复后JSON: {}", result.length() > 200 ? result.substring(0, 200) + "..." : result);
        return result;
    }
    
    private String cleanAndFixJson(String json) {
        if (json == null || json.isEmpty()) {
            return "{}";
        }
        
        // 提取完整的JSON对象
        if (json.contains("{")) {
            int jsonStart = json.indexOf("{");
            int jsonEnd = findMatchingBrace(json, jsonStart);
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                json = json.substring(jsonStart, jsonEnd + 1);
            }
        }
        
        // 尝试修复不完整的JSON
        json = fixIncompleteJson(json);
        
        return json;
    }
    
    /**
     * 查找匹配的大括号位置
     * @param text 文本内容
     * @param start 开始位置（左大括号位置）
     * @return 匹配的右大括号位置，如果没有找到返回-1
     */
    private int findMatchingBrace(String text, int start) {
        if (start < 0 || start >= text.length() || text.charAt(start) != '{') {
            return -1;
        }
        
        int braceCount = 1;
        boolean inString = false;
        boolean escape = false;
        
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (escape) {
                escape = false;
                continue;
            }
            
            if (c == '\\') {
                escape = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        return i;
                    }
                }
            }
        }
        
        // 如果没有找到匹配的右大括号，返回最后一个字符位置
        return text.length() - 1;
    }

    /**
     * 尝试修复不完整的JSON
     * @param json 可能不完整的JSON字符串
     * @return 修复后的JSON字符串
     */
    private String fixIncompleteJson(String json) {
        if (json == null || json.isEmpty()) {
            return "{}";
        }
        
        // 首先尝试构建一个有效的JSON结构
        String result = buildValidJsonStructure(json);
        
        // 尝试解析验证
        ObjectMapper tempMapper = new ObjectMapper();
        try {
            tempMapper.readTree(result);
            return result;
        } catch (Exception e) {
            // 如果解析失败，尝试提取最大的有效JSON对象
            int firstBrace = result.indexOf('{');
            if (firstBrace != -1) {
                int matchingBrace = findMatchingBrace(result, firstBrace);
                if (matchingBrace != -1 && matchingBrace > firstBrace) {
                    String extracted = result.substring(firstBrace, matchingBrace + 1);
                    try {
                        tempMapper.readTree(extracted);
                        return extracted;
                    } catch (Exception e2) {
                        // 如果提取的也失败，尝试最小化修复
                        return minimalJsonFix(extracted);
                    }
                }
            }
            // 如果所有尝试都失败，尝试最小化修复
            return minimalJsonFix(result);
        }
    }
    
    /**
     * 最小化修复JSON，尝试从截断的JSON中提取有效数据
     * @param json 原始JSON字符串（可能截断）
     * @return 修复后的JSON字符串
     */
    private String minimalJsonFix(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "{\"examPapers\":[]}";
        }
        
        String trimmed = json.trim();
        log.debug("尝试最小化修复JSON，原始长度: {}", trimmed.length());
        
        try {
            // 1. 尝试提取完整的试卷对象
            List<Map<String, Object>> papers = new ArrayList<>();
            
            // 查找所有试卷对象的开始位置
            int searchPos = 0;
            while (true) {
                int paperStart = trimmed.indexOf("\"id\"", searchPos);
                if (paperStart == -1) paperStart = trimmed.indexOf("\"paperName\"", searchPos);
                if (paperStart == -1) break;
                
                // 找到这个对象的开始位置（向前找"{"）
                int objStart = trimmed.lastIndexOf("{", paperStart);
                if (objStart == -1) break;
                
                // 尝试提取这个对象
                Map<String, Object> paper = extractPaperObject(trimmed, objStart);
                if (!paper.isEmpty()) {
                    papers.add(paper);
                }
                
                // 继续搜索下一个
                searchPos = paperStart + 1;
            }
            
            // 2. 如果提取到了试卷，构建完整的JSON
            if (!papers.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> result = new HashMap<>();
                result.put("examPapers", papers);
                return mapper.writeValueAsString(result);
            }
            
            // 3. 如果没有提取到完整对象，尝试提取部分字段
            Map<String, Object> partialPaper = extractPartialPaperFields(trimmed);
            if (!partialPaper.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> result = new HashMap<>();
                result.put("examPapers", Arrays.asList(partialPaper));
                return mapper.writeValueAsString(result);
            }
            
        } catch (Exception e) {
            log.warn("最小化修复失败: {}", e.getMessage());
        }
        
        // 4. 如果所有尝试都失败，返回空数组
        return "{\"examPapers\":[]}";
    }
    
    /**
     * 从指定位置提取试卷对象
     */
    private Map<String, Object> extractPaperObject(String json, int startPos) {
        Map<String, Object> paper = new HashMap<>();
        
        try {
            // 提取id
            String id = extractStringField(json, "id", startPos);
            if (id != null) paper.put("id", id);
            
            // 提取year
            String year = extractStringField(json, "year", startPos);
            if (year != null) paper.put("year", year);
            
            // 提取paperName
            String paperName = extractStringField(json, "paperName", startPos);
            if (paperName != null) paper.put("paperName", paperName);
            
            // 提取subjectId
            String subjectId = extractStringField(json, "subjectId", startPos);
            if (subjectId != null) paper.put("subjectId", subjectId);
            
            // 提取subjectName
            String subjectName = extractStringField(json, "subjectName", startPos);
            if (subjectName != null) paper.put("subjectName", subjectName);
            
            // 提取数值字段
            Integer duration = extractIntField(json, "duration", startPos);
            if (duration != null) paper.put("duration", duration);
            
            Integer totalScore = extractIntField(json, "totalScore", startPos);
            if (totalScore != null) paper.put("totalScore", totalScore);
            
            Integer questionCount = extractIntField(json, "questionCount", startPos);
            if (questionCount != null) paper.put("questionCount", questionCount);
            
            Integer difficulty = extractIntField(json, "difficulty", startPos);
            if (difficulty != null) paper.put("difficulty", difficulty);
            
            // 提取recommendReason
            String recommendReason = extractStringField(json, "recommendReason", startPos);
            if (recommendReason != null) paper.put("recommendReason", recommendReason);
            
            // 提取source
            String source = extractStringField(json, "source", startPos);
            if (source != null) paper.put("source", source);
            
            // 提取questions数组（如果存在）
            List<Map<String, Object>> questions = extractQuestionsArray(json, startPos);
            if (questions != null && !questions.isEmpty()) {
                paper.put("questions", questions);
            }
            
        } catch (Exception e) {
            log.debug("提取试卷对象失败: {}", e.getMessage());
        }
        
        return paper;
    }
    
    /**
     * 提取字符串字段
     */
    private String extractStringField(String json, String fieldName, int searchPos) {
        try {
            int fieldPos = json.indexOf("\"" + fieldName + "\"", searchPos);
            if (fieldPos == -1) return null;
            
            int colonPos = json.indexOf(":", fieldPos);
            if (colonPos == -1) return null;
            
            // 跳过空白字符
            int valueStart = colonPos + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }
            
            // 检查是否是字符串
            if (valueStart < json.length() && json.charAt(valueStart) == '"') {
                valueStart++; // 跳过开始的引号
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueEnd != -1) {
                    return json.substring(valueStart, valueEnd);
                }
            }
        } catch (Exception e) {
            log.debug("提取字符串字段 {} 失败: {}", fieldName, e.getMessage());
        }
        return null;
    }
    
    /**
     * 提取整数字段
     */
    private Integer extractIntField(String json, String fieldName, int searchPos) {
        try {
            int fieldPos = json.indexOf("\"" + fieldName + "\"", searchPos);
            if (fieldPos == -1) return null;
            
            int colonPos = json.indexOf(":", fieldPos);
            if (colonPos == -1) return null;
            
            // 跳过空白字符
            int valueStart = colonPos + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }
            
            // 提取数字
            StringBuilder numStr = new StringBuilder();
            while (valueStart < json.length() && (Character.isDigit(json.charAt(valueStart)) || json.charAt(valueStart) == '-')) {
                numStr.append(json.charAt(valueStart));
                valueStart++;
            }
            
            if (numStr.length() > 0) {
                return Integer.parseInt(numStr.toString());
            }
        } catch (Exception e) {
            log.debug("提取整数字段 {} 失败: {}", fieldName, e.getMessage());
        }
        return null;
    }
    
    /**
     * 提取questions数组
     */
    private List<Map<String, Object>> extractQuestionsArray(String json, int searchPos) {
        List<Map<String, Object>> questions = new ArrayList<>();
        
        try {
            int questionsPos = json.indexOf("\"questions\"", searchPos);
            if (questionsPos == -1) return questions;
            
            int colonPos = json.indexOf(":", questionsPos);
            if (colonPos == -1) return questions;
            
            int arrayStart = json.indexOf("[", colonPos);
            if (arrayStart == -1) return questions;
            
            // 查找数组中的所有对象
            int objStart = arrayStart;
            while (true) {
                objStart = json.indexOf("{", objStart + 1);
                if (objStart == -1) break;
                
                Map<String, Object> question = extractQuestionObject(json, objStart);
                if (question != null && !question.isEmpty()) {
                    questions.add(question);
                }
            }
        } catch (Exception e) {
            log.debug("提取questions数组失败: {}", e.getMessage());
        }
        
        return questions;
    }
    
    /**
     * 提取题目对象
     */
    private Map<String, Object> extractQuestionObject(String json, int startPos) {
        Map<String, Object> question = new HashMap<>();
        
        try {
            // 提取id
            String id = extractStringField(json, "id", startPos);
            if (id != null) question.put("id", id);
            
            // 提取type
            String type = extractStringField(json, "type", startPos);
            if (type != null) question.put("type", type);
            
            // 提取content
            String content = extractStringField(json, "content", startPos);
            if (content != null) {
                question.put("content", content);
                // 同时设置question字段，确保前端能正确显示
                question.put("question", content);
            }
            
            // 提取question字段（如果存在）
            String questionText = extractStringField(json, "question", startPos);
            if (questionText != null) {
                question.put("question", questionText);
                // 如果没有content字段，将question字段的值也赋值给content
                if (!question.containsKey("content")) {
                    question.put("content", questionText);
                }
            }
            
            // 提取correctAnswer
            Integer correctAnswer = extractIntField(json, "correctAnswer", startPos);
            if (correctAnswer != null) question.put("correctAnswer", correctAnswer);
            
            // 提取analysis
            String analysis = extractStringField(json, "analysis", startPos);
            if (analysis != null) question.put("analysis", analysis);
            
            // 提取score
            Integer score = extractIntField(json, "score", startPos);
            if (score != null) question.put("score", score);
            
        } catch (Exception e) {
            log.debug("提取题目对象失败: {}", e.getMessage());
        }
        
        return question;
    }
    
    /**
     * 提取部分试卷字段（当无法提取完整对象时使用）
     */
    private Map<String, Object> extractPartialPaperFields(String json) {
        Map<String, Object> paper = new HashMap<>();
        
        // 尝试提取paperName
        String paperName = extractStringField(json, "paperName", 0);
        if (paperName != null) paper.put("paperName", paperName);
        
        // 尝试提取year
        String year = extractStringField(json, "year", 0);
        if (year != null) paper.put("year", year);
        
        // 尝试提取id
        String id = extractStringField(json, "id", 0);
        if (id != null) paper.put("id", id);
        
        return paper;
    }
    
    /**
     * 构建有效的JSON结构
     * @param json 原始JSON字符串
     * @return 构建后的JSON字符串
     */
    private String buildValidJsonStructure(String json) {
        StringBuilder fixed = new StringBuilder();
        
        boolean inString = false;
        boolean escape = false;
        Stack<Character> stack = new Stack<>();
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escape) {
                fixed.append(c);
                escape = false;
                continue;
            }
            
            if (c == '\\') {
                fixed.append(c);
                escape = true;
                continue;
            }
            
            if (c == '"') {
                fixed.append(c);
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{' || c == '[') {
                    fixed.append(c);
                    stack.push(c);
                } else if (c == '}') {
                    // 只有当栈顶是 { 时才添加
                    if (!stack.isEmpty() && stack.peek() == '{') {
                        fixed.append(c);
                        stack.pop();
                    }
                } else if (c == ']') {
                    // 只有当栈顶是 [ 时才添加
                    if (!stack.isEmpty() && stack.peek() == '[') {
                        fixed.append(c);
                        stack.pop();
                    }
                } else {
                    fixed.append(c);
                }
            } else {
                fixed.append(c);
            }
        }
        
        // 关闭所有未关闭的结构
        while (!stack.isEmpty()) {
            char open = stack.pop();
            if (open == '{') {
                fixed.append('}');
            } else if (open == '[') {
                fixed.append(']');
            }
        }
        
        // 确保字符串正确关闭
        if (inString) {
            fixed.append('"');
        }
        
        // 清理结尾的逗号
        String result = fixed.toString().trim();
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        
        return result;
    }

    /**
     * 清理URL中的多余字符，保留原始链接
     * @param url 原始URL
     * @return 清理后的URL
     */
    private String cleanUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        // 去除首尾空白字符
        url = url.trim();
        
        // 去除可能的引号
        if ((url.startsWith("'") && url.endsWith("'")) || 
            (url.startsWith("\"") && url.endsWith("\""))) {
            url = url.substring(1, url.length() - 1);
        }
        
        // 去除可能的Markdown格式
        url = url.replaceAll("\\[\\]\\(\\)", "");
        
        // 确保URL以http://或https://开头
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // 如果AI返回的URL不以http开头，尝试添加https://
            if (url.startsWith("www.")) {
                url = "https://" + url;
            }
        }
        
        return url;
    }

    @Override
    public void saveProcessedText(String question, String processedText, String subject, Long userId) {
        try {
            if (userId == null) {
                log.warn("userId为null，无法保存历史记录");
                throw new RuntimeException("用户未登录");
            }
            
            log.info("用户ID {} 保存处理后的文本", userId);
            
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.error("用户ID {} 不存在", userId);
                throw new RuntimeException("用户不存在");
            }
            
            // 对输入进行安全处理，防止XSS攻击
            String sanitizedQuestion = sanitizeInput(question);
            String sanitizedProcessedText = sanitizeInput(processedText);
            String sanitizedSubject = sanitizeInput(subject);
            
            QaHistory history = new QaHistory();
            history.setUserId(user.getId());
            history.setQuestion(sanitizedQuestion);
            history.setAnswer(sanitizedProcessedText);
            history.setContext(sanitizedSubject);
            history.setQuestionType("PROCESSED");
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());
            
            qaHistoryMapper.insert(history);
            log.info("处理后的文本保存成功，ID: {}", history.getId());
            
            // Redis缓存处理
            String historyKey = "qa:history:" + user.getId() + ":" + history.getId();
            String processedTextKey = "qa:processed:" + user.getId() + ":" + history.getId();
            String userHistoryListKey = "qa:user:history:" + user.getId();
            
            // 缓存历史记录
            redisTemplate.opsForValue().set(historyKey, history, java.time.Duration.ofHours(24));
            
            // 缓存处理后的文本
            redisTemplate.opsForValue().set(processedTextKey, sanitizedProcessedText, java.time.Duration.ofHours(24));
            
            // 添加到用户历史记录列表
            redisTemplate.opsForList().leftPush(userHistoryListKey, history.getId());
            
            // 限制用户历史记录列表长度（保留最近100条）
            redisTemplate.opsForList().trim(userHistoryListKey, 0, 99);
            
            log.info("Redis缓存成功，历史记录ID: {}", history.getId());
        } catch (Exception e) {
            log.error("保存处理后的文本失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存处理后的文本失败: " + e.getMessage());
        }
    }

    @Override
    public void saveProcessedText(String question, String processedText, String subject, Long userId, String sessionId) {
        try {
            if (userId == null) {
                log.warn("userId为null，无法保存历史记录");
                throw new RuntimeException("用户未登录");
            }
            
            log.info("用户ID {} 保存处理后的文本，会话ID: {}", userId, sessionId);
            
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.error("用户ID {} 不存在", userId);
                throw new RuntimeException("用户不存在");
            }
            
            String sanitizedQuestion = sanitizeInput(question);
            String sanitizedProcessedText = sanitizeInput(processedText);
            String sanitizedSubject = sanitizeInput(subject);
            
            QaHistory history = new QaHistory();
            history.setUserId(user.getId());
            history.setSessionId(sessionId);
            history.setQuestion(sanitizedQuestion);
            history.setAnswer(sanitizedProcessedText);
            history.setContext(sanitizedSubject);
            history.setQuestionType("PROCESSED");
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());
            
            qaHistoryMapper.insert(history);
            log.info("处理后的文本保存成功，ID: {}, 会话ID: {}", history.getId(), sessionId);
            
            String historyKey = "qa:history:" + user.getId() + ":" + history.getId();
            String processedTextKey = "qa:processed:" + user.getId() + ":" + history.getId();
            String userHistoryListKey = "qa:user:history:" + user.getId();
            
            redisTemplate.opsForValue().set(historyKey, history, java.time.Duration.ofHours(24));
            redisTemplate.opsForValue().set(processedTextKey, sanitizedProcessedText, java.time.Duration.ofHours(24));
            redisTemplate.opsForList().leftPush(userHistoryListKey, history.getId());
            redisTemplate.opsForList().trim(userHistoryListKey, 0, 99);
            
            log.info("Redis缓存成功，历史记录ID: {}", history.getId());
        } catch (Exception e) {
            log.error("保存处理后的文本失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存处理后的文本失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存答疑历史记录的实现方法
     * 
     * 处理流程：
     * 1. 参数验证：检查userId是否为空
     * 2. 安全处理：对所有文本输入进行XSS防护，防止恶意脚本注入
     * 3. 创建实体：构建QaHistory对象
     * 4. 数据持久化：插入到数据库
     * 5. 异常处理：捕获并记录错误，但不抛出异常，避免影响主流程
     * 
     * 安全防护措施：
     * - 对question（问题）进行HTML转义
     * - 对answer（回答）进行HTML转义
     * - 对context（上下文）进行HTML转义
     * - 对questionType（问题类型）进行HTML转义
     * 
     * XSS防护示例：
     * - <script> → &lt;script&gt;
     * - & → &amp;
     * - " → &quot;
     * - ' → &#039;
     * 
     * @param userId 用户ID（必需）
     * @param question 用户问题
     * @param answer AI回答
     * @param context 上下文信息
     * @param bookId 关联教材ID（可选）
     * @param knowledgeNodeId 关联知识点ID（可选）
     * @param questionType 问题类型（可选，默认NORMAL）
     */
    @Override
    public void saveHistory(Long userId, String question, String answer, String context, 
                           Long bookId, Long knowledgeNodeId, String questionType) {
        try {
            // 步骤1：参数验证
            if (userId == null) {
                log.warn("userId为null，无法保存答疑历史");
                return;
            }
            
            log.info("保存答疑历史，用户ID: {}", userId);
            
            // 步骤2：安全处理 - 对所有文本输入进行XSS防护
            // 这是很重要的安全措施，防止用户输入的恶意脚本被存储和执行
            String sanitizedQuestion = sanitizeInput(question);
            String sanitizedAnswer = sanitizeInput(answer);
            String sanitizedContext = sanitizeInput(context);
            // 如果questionType为空，使用默认值"NORMAL"
            String sanitizedQuestionType = questionType != null ? sanitizeInput(questionType) : "NORMAL";
            
            // 步骤3：创建QaHistory实体对象
            QaHistory history = new QaHistory();
            history.setUserId(userId);                    // 设置用户ID
            history.setQuestion(sanitizedQuestion);        // 设置处理后的用户问题
            history.setAnswer(sanitizedAnswer);           // 设置处理后的AI回答
            history.setContext(sanitizedContext);         // 设置处理后的上下文
            history.setQuestionType(sanitizedQuestionType); // 设置处理后的问题类型
            history.setBookId(bookId);                    // 设置关联的教材ID（可为null）
            history.setKnowledgeNodeId(knowledgeNodeId);  // 设置关联的知识点ID（可为null）
            history.setCreatedAt(LocalDateTime.now());    // 设置创建时间
            history.setUpdatedAt(LocalDateTime.now());    // 设置更新时间
            
            // 步骤4：数据持久化 - 插入到数据库
            qaHistoryMapper.insert(history);
            log.info("答疑历史保存成功，ID: {}", history.getId());
            
        } catch (Exception e) {
            // 步骤5：异常处理
            // 记录详细的错误信息，便于排查问题
            log.error("保存答疑历史失败: {}", e.getMessage(), e);
            // 注意：这里不抛出异常，是为了避免影响主流程
            // 流式响应已经完成，主要功能不受影响，保存失败只是丢失了一条历史记录
        }
    }
    
    @Override
    public void saveHistory(Long userId, String sessionId, String question, String answer, 
                           String context, Long bookId, Long knowledgeNodeId, String questionType) {
        try {
            if (userId == null) {
                log.warn("userId为null，无法保存答疑历史");
                return;
            }
            
            // 记录详细的保存日志，便于排查sessionId问题
            if (sessionId == null || sessionId.isEmpty()) {
                log.warn("保存答疑历史时sessionId为空，用户ID: {}, 问题: {}", userId, 
                    question != null && question.length() > 50 ? question.substring(0, 50) + "..." : question);
            } else {
                log.info("保存答疑历史，用户ID: {}, 会话ID: {}, 问题长度: {}", userId, sessionId, 
                    question != null ? question.length() : 0);
            }
            
            String sanitizedQuestion = sanitizeInput(question);
            String sanitizedAnswer = sanitizeInput(answer);
            String sanitizedContext = sanitizeInput(context);
            String sanitizedQuestionType = questionType != null ? sanitizeInput(questionType) : "NORMAL";
            
            QaHistory history = new QaHistory();
            history.setUserId(userId);
            history.setSessionId(sessionId);
            history.setQuestion(sanitizedQuestion);
            history.setAnswer(sanitizedAnswer);
            history.setContext(sanitizedContext);
            history.setQuestionType(sanitizedQuestionType);
            history.setBookId(bookId);
            history.setKnowledgeNodeId(knowledgeNodeId);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());
            
            qaHistoryMapper.insert(history);
            log.info("答疑历史保存成功，ID: {}, 会话ID: {}", history.getId(), sessionId);
            
        } catch (Exception e) {
            log.error("保存答疑历史失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 输入安全过滤方法 - 防止XSS跨站脚本攻击
     * 
     * 功能说明：
     * 将用户输入中的特殊HTML字符转换为安全的HTML实体，防止恶意脚本注入
     * 
     * 转换规则：
     * 1. & (ampersand) → &amp;amp;     防止HTML实体注入
     * 2. < (小于号) → &amp;lt;         防止HTML标签开始
     * 3. > (大于号) → &amp;gt;         防止HTML标签结束
     * 4. " (双引号) → &amp;quot;         防止属性注入
     * 5. ' (单引号) → &amp;#039;        防止属性注入
     * 
     * 使用场景：
     * - 保存用户输入到数据库前
     * - 显示用户输入到页面之前
     * - 处理富文本编辑器内容
     * 
     * 示例：
     * 输入: <script>alert('XSS')</script>
     * 输出: &amp;lt;script&amp;gt;alert(&amp;#039;XSS&amp;#039;)&amp;lt;/script&amp;gt;
     * 
     * 注意：
     * 如果需要保留HTML格式（如富文本），应该使用白名单过滤而不是完全转义
     * 
     * @param input 原始输入字符串
     * @return 过滤后的安全字符串，如果输入为null则返回null
     */
    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input
            .replace("&", "&amp;")      // 必须最先处理&amp;，避免二次转义
            .replace("<", "&lt;")        // HTML标签开始
            .replace(">", "&gt;")       // HTML标签结束
            .replace("\"", "&quot;")    // 双引号，用于属性值
            .replace("'", "&#039;");    // 单引号，用于属性值
    }




    /**
     * 生成课程推荐的JSON数据结构
     * @return 课程推荐JSON数据
     */
    public Map<String, Object> getCourseRecommendations() {
        try {
            log.info("生成课程推荐JSON数据");
            
            // 获取最近的问答记录作为上下文
            String recentQaContext = getRecentQaContext();
            boolean hasQaHistory = !recentQaContext.isEmpty();
            
            // 生成缓存键，基于问答内容的哈希值
            String cacheKey = null;
            if (hasQaHistory) {
                cacheKey = "course:recommendations:" + generateContextHash(recentQaContext);
                
                // 尝试从Redis获取缓存
                try {
                    Object cachedResult = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedResult != null) {
                        log.info("从Redis缓存获取课程推荐数据");
                        return (Map<String, Object>) cachedResult;
                    }
                } catch (Exception e) {
                    log.warn("从Redis获取缓存失败: {}", e.getMessage());
                }
            }
            
            Map result = new HashMap<>();

            // 联网真实搜索：从最近学习情况提取知识点关键词，再调用B站搜索接口获取真实视频候选
            List<Map<String, Object>> videoCandidates = Collections.emptyList();
            List<String> keywords = extractKeywordsFromContext(recentQaContext, hasQaHistory);
            if (!keywords.isEmpty()) {
                videoCandidates = bilibiliVideoSearchService.searchByKeywords(keywords, 4);
                log.info("B站真实搜索获得候选视频 {} 条，关键词: {}", videoCandidates.size(), keywords);
            }
            final List<Map<String, Object>> candidates = videoCandidates;
            final Set<String> realUrls = new HashSet<>();
            for (Map<String, Object> c : candidates) {
                realUrls.add(String.valueOf(c.get("url")));
            }

            // 构建AI提示词（注入真实候选，强制链接只能原样引用）
            StringBuilder candidateText = new StringBuilder();
            if (!candidates.isEmpty()) {
                candidateText.append("以下是通过B站搜索接口真实检索到的视频候选（链接真实有效）：\n\n");
                for (int i = 0; i < candidates.size(); i++) {
                    Map<String, Object> c = candidates.get(i);
                    candidateText.append(String.format("[%d] 标题: %s | UP主: %s | 播放量: %s | 时长: %s | 链接: %s%n    简介: %s%n",
                            i + 1, c.get("title"), c.get("author"), c.get("play"), c.get("duration"),
                            c.get("url"), c.get("description")));
                }
            }

            String jsonPrompt = "基于用户最近的学习情况，生成一个课程推荐的JSON数据结构，包含以下内容：\n" +
                    "{\n" +
                    "  \"categories\": [\n" +
                    "    {\n" +
                    "      \"name\": \"官方平台体系化课程\",\n" +
                    "      \"description\": \"由名校名师讲授，内容完整、结构严谨，适合系统学习、打牢基础。\",\n" +
                    "      \"courses\": [\n" +
                    "        {\n" +
                    "          \"name\": \"课程名称\",\n" +
                    "          \"instructors\": [\"讲师1\", \"讲师2\"],\n" +
                    "          \"institution\": \"所属机构\",\n" +
                    "          \"platform\": \"平台名称\",\n" +
                    "          \"video_url\": \"视频链接\",\n" +
                    "          \"cover_url\": \"封面图片链接\",\n" +
                    "          \"features\": \"课程特点\",\n" +
                    "          \"target_audience\": \"目标受众\"\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"B站UP主灵活专题课\",\n" +
                    "      \"description\": \"适合快速攻克难点、考前突击或学习解题技巧。\",\n" +
                    "      \"courses\": [\n" +
                    "        {\n" +
                    "          \"up_name\": \"UP主名称\",\n" +
                    "          \"institution\": \"机构信息\",\n" +
                    "          \"platform\": \"Bilibili\",\n" +
                    "          \"video_url\": \"视频链接\",\n" +
                    "          \"cover_url\": \"封面图片链接\",\n" +
                    "          \"series\": [\n" +
                    "            {\n" +
                    "              \"name\": \"系列名称\",\n" +
                    "              \"url\": \"系列链接\"\n" +
                    "            }\n" +
                    "          ],\n" +
                    "          \"features\": \"课程特点\",\n" +
                    "          \"target_audience\": \"目标受众\"\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"selection_tips\": [\n" +
                    "    {\n" +
                    "      \"scenario\": \"场景描述\",\n" +
                    "      \"suggestion\": \"建议内容\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n" +
                    "硬性要求：\n" +
                    "1. 所有 video_url 和 series[].url 只能从下方候选列表中原样复制，严禁编造、拼接或修改任何链接\n" +
                    "2. 若候选不足，宁可少推荐几个课程，也不要输出任何不在候选列表中的链接\n" +
                    "3. 优先选择播放量高、UP主权威（如大学官方账号、名师）、标题与用户学习内容强相关的候选\n" +
                    "4. 每个类别至少包含2个课程（候选足够时）\n" +
                    "5. 选择建议至少包含4个不同场景\n" +
                    "6. 直接返回JSON格式，不要包含其他解释性文本\n" +
                    "\n" + candidateText +
                    "\n用户最近的学习情况：\n" + recentQaContext;
            
            // 调用AI服务生成推荐
            String aiResponse = aiService.generateResponseWithContext(recentQaContext, jsonPrompt);
            aiResponse = preprocessAiResponse(aiResponse);
            
            // 解析AI返回的JSON
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                result = objectMapper.readValue(aiResponse, Map.class);
            } catch (Exception e) {
                log.warn("AI返回的响应不是有效的JSON，使用默认推荐: {}", e.getMessage());
                log.debug("AI响应内容: {}", aiResponse);
                return getDefaultCourseRecommendations();
            }
            
            // 强制校验视频链接：不在真实候选集合中的链接一律替换为对应主题的B站搜索页链接
            if (result.containsKey("categories")) {
                List<Map<String, Object>> categories = (List<Map<String, Object>>) result.get("categories");
                for (Map<String, Object> category : categories) {
                    if (category.containsKey("courses")) {
                        List<Map<String, Object>> courses = (List<Map<String, Object>>) category.get("courses");
                        for (Map<String, Object> course : courses) {
                            // 校验课程视频链接
                            if (course.containsKey("video_url")) {
                                String videoUrl = (String) course.get("video_url");
                                String fixedUrl = enforceRealUrl(videoUrl, realUrls, course);
                                if (!fixedUrl.equals(videoUrl)) {
                                    log.warn("课程 {} 的链接不在真实候选中（{}），已替换为: {}", course.get("name"), videoUrl, fixedUrl);
                                    course.put("video_url", fixedUrl);
                                }
                            }
                            // 校验B站课程的系列链接
                            if (course.containsKey("series")) {
                                List<Map<String, Object>> series = (List<Map<String, Object>>) course.get("series");
                                for (Map<String, Object> seriesItem : series) {
                                    if (seriesItem.containsKey("url")) {
                                        String seriesUrl = (String) seriesItem.get("url");
                                        String fixedUrl = enforceRealUrl(seriesUrl, realUrls, seriesItem);
                                        if (!fixedUrl.equals(seriesUrl)) {
                                            log.warn("系列 {} 的链接不在真实候选中（{}），已替换为: {}",
                                                    seriesItem.get("name"), seriesUrl, fixedUrl);
                                            seriesItem.put("url", fixedUrl);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 如果AI返回的结构不完整，添加必要的字段
            if (!result.containsKey("note")) {
                result.put("note", "cover_url字段暂为空，因搜索结果中未提供具体的课程封面图片链接。部分series的url字段为空，因该系列包含多个视频，建议访问UP主主页查看完整合集。");
            }
            
            // 打印详细的推荐信息
            log.info("\n=========================================");
            log.info("          网课推荐详细信息");
            log.info("=========================================");
            
            if (result.containsKey("categories")) {
                List<Map<String, Object>> categories = (List<Map<String, Object>>) result.get("categories");
                for (int i = 0; i < categories.size(); i++) {
                    Map<String, Object> category = categories.get(i);
                    log.info("\n【分类 {}】: {}", i + 1, category.get("name"));
                    log.info("分类描述: {}", category.get("description"));
                    
                    if (category.containsKey("courses")) {
                        List<Map<String, Object>> courses = (List<Map<String, Object>>) category.get("courses");
                        for (int j = 0; j < courses.size(); j++) {
                            Map<String, Object> course = courses.get(j);
                            log.info("\n  课程 {}: {}", j + 1, course.get("name"));
                            
                            // 打印课程详细信息
                            if (course.containsKey("instructors")) {
                                log.info("  讲师: {}", course.get("instructors"));
                            }
                            if (course.containsKey("up_name")) {
                                log.info("  UP主: {}", course.get("up_name"));
                            }
                            if (course.containsKey("institution")) {
                                log.info("  机构: {}", course.get("institution"));
                            }
                            if (course.containsKey("platform")) {
                                log.info("  平台: {}", course.get("platform"));
                            }
                            if (course.containsKey("video_url")) {
                                log.info("  视频链接: {}", course.get("video_url"));
                            }
                            if (course.containsKey("features")) {
                                log.info("  课程特点: {}", course.get("features"));
                            }
                            if (course.containsKey("target_audience")) {
                                log.info("  目标受众: {}", course.get("target_audience"));
                            }
                            
                            // 打印B站系列课程
                            if (course.containsKey("series")) {
                                List<Map<String, Object>> series = (List<Map<String, Object>>) course.get("series");
                                log.info("  系列课程:");
                                for (int k = 0; k < series.size(); k++) {
                                    Map<String, Object> seriesItem = series.get(k);
                                    log.info("    系列 {}: {}", k + 1, seriesItem.get("name"));
                                    if (seriesItem.containsKey("url")) {
                                        log.info("    系列链接: {}", seriesItem.get("url"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 打印选择建议
            if (result.containsKey("selection_tips")) {
                List<Map<String, Object>> selectionTips = (List<Map<String, Object>>) result.get("selection_tips");
                log.info("\n【选择建议】");
                for (int i = 0; i < selectionTips.size(); i++) {
                    Map<String, Object> tip = selectionTips.get(i);
                    log.info("  场景 {}: {}", i + 1, tip.get("scenario"));
                    log.info("  建议: {}", tip.get("suggestion"));
                }
            }
            
            // 打印备注信息
            if (result.containsKey("note")) {
                log.info("\n【备注】");
                log.info("  {}", result.get("note"));
            }
            
            log.info("=========================================");
            log.info("          推荐信息打印完成");
            log.info("=========================================\n");
            
            // 将结果存入Redis缓存（如果缓存键不为空）
            if (cacheKey != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, result, java.time.Duration.ofHours(24));
                    log.info("课程推荐数据已缓存到Redis，缓存键: {}", cacheKey);
                } catch (Exception e) {
                    log.warn("缓存课程推荐数据到Redis失败: {}", e.getMessage());
                }
            }
            
            log.info("生成课程推荐JSON数据成功");
            return result;
        } catch (Exception e) {
            log.error("生成课程推荐JSON数据失败: {}", e.getMessage(), e);
            // 失败时返回默认推荐
            return getDefaultCourseRecommendations();
        }
    }
    
    /**
     * 从学习上下文中提取知识点关键词（用于B站真实搜索）
     */
    private List<String> extractKeywordsFromContext(String context, boolean hasQaHistory) {
        if (!hasQaHistory || context == null || context.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String prompt = "请从以下学习内容中提取3个最核心的知识点关键词（用于在B站搜索教学视频），"
                    + "用逗号分隔，只返回关键词本身，不要任何解释：\n" + context;
            String response = aiService.generateResponseWithContext(
                    "你是一个知识点提取助手，专门从学习内容中提取核心知识点关键词", prompt);
            if (response == null || response.isEmpty()) {
                return Collections.emptyList();
            }
            return Arrays.stream(response.split("[,，、\n]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.length() <= 30)
                    .limit(3)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("提取知识点关键词失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 强制链接真实：候选集合中的链接原样保留；B站搜索页链接保留；其余替换为主题搜索页链接
     */
    private String enforceRealUrl(String url, Set<String> realUrls, Map<String, Object> owner) {
        if (url != null && !url.isEmpty() && realUrls.contains(url)) {
            return url;
        }
        if (url != null && url.startsWith("https://search.bilibili.com/")) {
            return url;
        }
        String keyword = String.valueOf(owner.getOrDefault("name",
                owner.getOrDefault("up_name", "大学课程")));
        return bilibiliVideoSearchService.searchPageUrl(keyword);
    }

    /**
     * 生成上下文的哈希值，用于缓存键
     * @param context 上下文内容
     * @return 哈希值字符串
     */
    private String generateContextHash(String context) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(context.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 如果哈希生成失败，使用时间戳作为备选
            return String.valueOf(System.currentTimeMillis());
        }
    }
    
    /**
     * 获取默认课程推荐（当AI调用失败时使用）
     * @return 默认课程推荐JSON数据
     */
    private Map<String, Object> getDefaultCourseRecommendations() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> categories = new ArrayList<>();
        
        // 官方平台体系化课程
        Map<String, Object> officialCategory = new HashMap<>();
        officialCategory.put("name", "官方平台体系化课程");
        officialCategory.put("description", "由名校名师讲授，内容完整、结构严谨，适合系统学习、打牢基础。");
        
        List<Map<String, Object>> officialCourses = new ArrayList<>();
        
        // 高等数学（上）- 智慧树
        Map<String, Object> mathCourse = new HashMap<>();
        mathCourse.put("name", "高等数学（上）");
        mathCourse.put("instructors", java.util.Arrays.asList("吴月宁", "吴养会", "解小莉", "郑立飞", "陈小蕾", "王建瑜"));
        mathCourse.put("institution", "西北农林科技大学");
        mathCourse.put("platform", "智慧树");
        mathCourse.put("video_url", "https://coursehome.zhihuishu.com/courseHome/1000080776");
        mathCourse.put("cover_url", "https://picsum.photos/320/180?random=math1");
        mathCourse.put("features", "名师联合授课，内容权威，强调抽象思维和实际应用能力。课程设计65个知识点视频，大部分不超过15分钟。");
        mathCourse.put("target_audience", "工科、理科专业学生，作为主修课学习。");
        officialCourses.add(mathCourse);
        
        // 工科数学分析（一）- 中国大学MOOC
        Map<String, Object> analysisCourse = new HashMap<>();
        analysisCourse.put("name", "工科数学分析（一）");
        analysisCourse.put("instructors", java.util.Arrays.asList("杨小远"));
        analysisCourse.put("institution", "北京航空航天大学");
        analysisCourse.put("platform", "中国大学MOOC");
        analysisCourse.put("video_url", "https://www.icourse163.org/spoc/course/BUAA-1467546195");
        analysisCourse.put("cover_url", "https://picsum.photos/320/180?random=math2");
        analysisCourse.put("features", "内容更深、要求更高，设有\"提高课\"板块，引入现代数学视角和实际工程案例。充分利用多媒体技术将复杂数学问题直观化。");
        analysisCourse.put("target_audience", "数学基础较好、希望挑战更高难度的工科学生。");
        officialCourses.add(analysisCourse);
        
        // 线性代数 - 中国大学MOOC
        Map<String, Object> linearAlgebraCourse = new HashMap<>();
        linearAlgebraCourse.put("name", "线性代数");
        linearAlgebraCourse.put("instructors", java.util.Arrays.asList("王磊", "张明"));
        linearAlgebraCourse.put("institution", "清华大学");
        linearAlgebraCourse.put("platform", "中国大学MOOC");
        linearAlgebraCourse.put("video_url", "https://www.icourse163.org/course/TsinghuaX-10683");
        linearAlgebraCourse.put("cover_url", "https://picsum.photos/320/180?random=linear");
        linearAlgebraCourse.put("features", "系统讲解线性代数核心概念，包含矩阵运算、向量空间、特征值等重要内容，配套丰富例题和习题。");
        linearAlgebraCourse.put("target_audience", "理工科专业学生，考研备考学生。");
        officialCourses.add(linearAlgebraCourse);
        
        // 大学物理 - 智慧树
        Map<String, Object> physicsCourse = new HashMap<>();
        physicsCourse.put("name", "大学物理（上）");
        physicsCourse.put("instructors", java.util.Arrays.asList("张三", "李四"));
        physicsCourse.put("institution", "北京大学");
        physicsCourse.put("platform", "智慧树");
        physicsCourse.put("video_url", "https://coursehome.zhihuishu.com/courseHome/1000080777");
        physicsCourse.put("cover_url", "https://picsum.photos/320/180?random=physics");
        physicsCourse.put("features", "涵盖力学、热学、电磁学等基础物理内容，实验演示丰富，理论与实践结合。");
        physicsCourse.put("target_audience", "理工科专业学生，物理爱好者。");
        officialCourses.add(physicsCourse);
        
        // 考研英语 - 中国大学MOOC
        Map<String, Object> englishCourse = new HashMap<>();
        englishCourse.put("name", "考研英语全程班");
        englishCourse.put("instructors", java.util.Arrays.asList("王江涛", "唐迟"));
        englishCourse.put("institution", "新东方在线");
        englishCourse.put("platform", "中国大学MOOC");
        englishCourse.put("video_url", "https://www.icourse163.org/course/NEU-1000080778");
        englishCourse.put("cover_url", "https://picsum.photos/320/180?random=english");
        englishCourse.put("features", "系统讲解考研英语词汇、语法、阅读、写作等模块，提供大量真题解析和模拟练习。");
        englishCourse.put("target_audience", "考研学生，英语基础薄弱者。");
        officialCourses.add(englishCourse);
        
        officialCategory.put("courses", officialCourses);
        categories.add(officialCategory);
        
        // B站UP主灵活专题课
        Map<String, Object> bilibiliCategory = new HashMap<>();
        bilibiliCategory.put("name", "B站UP主灵活专题课");
        bilibiliCategory.put("description", "适合快速攻克难点、考前突击或学习解题技巧。");
        
        List<Map<String, Object>> bilibiliCourses = new ArrayList<>();
        
        // 一高数 UP主 - 数学
        Map<String, Object> yigaoshuCourse = new HashMap<>();
        yigaoshuCourse.put("up_name", "一高数");
        yigaoshuCourse.put("institution", "B站知名科普UP主");
        yigaoshuCourse.put("platform", "Bilibili");
        yigaoshuCourse.put("video_url", "https://space.bilibili.com/483544206");
        yigaoshuCourse.put("cover_url", "https://picsum.photos/320/180?random=yigaoshu");
        
        List<Map<String, Object>> yigaoshuSeries = new ArrayList<>();
        Map<String, Object> series1 = new HashMap<>();
        series1.put("name", "《高等数学(上/下)基础与解法全集》");
        series1.put("url", "https://www.bilibili.com/video/BV1mZ421T76c");
        yigaoshuSeries.add(series1);
        
        Map<String, Object> series2 = new HashMap<>();
        series2.put("name", "《线性代数基础与解法全集》");
        series2.put("url", "https://www.bilibili.com/video/BV1mZ421T76c");
        yigaoshuSeries.add(series2);
        
        yigaoshuCourse.put("series", yigaoshuSeries);
        yigaoshuCourse.put("features", "系统性强且实用，语言通俗易懂，串联考点和解题技巧。截至2025年3月粉丝数达149.2万，《线性代数基础与解法全集》播放量超463.5万。");
        yigaoshuCourse.put("target_audience", "适合快速入门、期末备考或考研复习。");
        bilibiliCourses.add(yigaoshuCourse);
        
        // 李永乐老师 - 考研数学
        Map<String, Object> liyongleCourse = new HashMap<>();
        liyongleCourse.put("up_name", "李永乐老师");
        liyongleCourse.put("institution", "清华大学数学系教授");
        liyongleCourse.put("platform", "Bilibili");
        liyongleCourse.put("video_url", "https://space.bilibili.com/9458053");
        liyongleCourse.put("cover_url", "https://picsum.photos/320/180?random=liyongle");
        
        List<Map<String, Object>> liyongleSeries = new ArrayList<>();
        Map<String, Object> series3 = new HashMap<>();
        series3.put("name", "《考研数学线性代数辅导》");
        series3.put("url", "https://www.bilibili.com/video/BV1Ab411G7Zq");
        liyongleSeries.add(series3);
        
        Map<String, Object> series4 = new HashMap<>();
        series4.put("name", "《考研数学基础班》");
        series4.put("url", "https://www.bilibili.com/video/BV1Xb411G7Zq");
        liyongleSeries.add(series4);
        
        liyongleCourse.put("series", liyongleSeries);
        liyongleCourse.put("features", "清华大学数学系教授，考研数学辅导专家，讲解深入浅出，重点突出，深受考研学生喜爱。");
        liyongleCourse.put("target_audience", "考研学生，数学基础薄弱者。");
        bilibiliCourses.add(liyongleCourse);
        
        // 张雪峰老师 - 考研规划
        Map<String, Object> zhangxuefengCourse = new HashMap<>();
        zhangxuefengCourse.put("up_name", "张雪峰老师");
        zhangxuefengCourse.put("institution", "考研辅导名师");
        zhangxuefengCourse.put("platform", "Bilibili");
        zhangxuefengCourse.put("video_url", "https://space.bilibili.com/385933515");
        zhangxuefengCourse.put("cover_url", "https://picsum.photos/320/180?random=zhangxuefeng");
        
        List<Map<String, Object>> zhangxuefengSeries = new ArrayList<>();
        Map<String, Object> series5 = new HashMap<>();
        series5.put("name", "《考研院校专业选择指南》");
        series5.put("url", "https://www.bilibili.com/video/BV1Xb411G7Zq");
        zhangxuefengSeries.add(series5);
        
        Map<String, Object> series6 = new HashMap<>();
        series6.put("name", "《考研备考规划》");
        series6.put("url", "https://www.bilibili.com/video/BV1Xb411G7Zq");
        zhangxuefengSeries.add(series6);
        
        zhangxuefengCourse.put("series", zhangxuefengSeries);
        zhangxuefengCourse.put("features", "知名考研辅导专家，擅长院校专业选择和备考规划，语言幽默风趣，实用性强。");
        zhangxuefengCourse.put("target_audience", "考研学生，对院校专业选择迷茫者。");
        bilibiliCourses.add(zhangxuefengCourse);
        
        bilibiliCategory.put("courses", bilibiliCourses);
        categories.add(bilibiliCategory);
        
        // 选择建议
        List<Map<String, Object>> selectionTips = new ArrayList<>();
        
        Map<String, Object> tip1 = new HashMap<>();
        tip1.put("scenario", "系统学习 / 基础薄弱");
        tip1.put("suggestion", "优先选择官方平台体系化课程，如《工科数学分析（一）》或《线性代数》（北京科技大学），跟随教学大纲逐步学习。");
        selectionTips.add(tip1);
        
        Map<String, Object> tip2 = new HashMap<>();
        tip2.put("scenario", "期末突击 / 考研复习");
        tip2.put("suggestion", "优先使用B站UP主的专题课程，例如\"一高数\"的解法全集，快速抓住重点与考点。");
        selectionTips.add(tip2);
        
        Map<String, Object> tip3 = new HashMap<>();
        tip3.put("scenario", "理解抽象概念");
        tip3.put("suggestion", "选择带有几何可视化或应用案例的课程，如北京科技大学《线性代数》。");
        selectionTips.add(tip3);
        
        Map<String, Object> tip4 = new HashMap<>();
        tip4.put("scenario", "利用平台优势");
        tip4.put("suggestion", "国家高等教育智慧教育平台课程权威性高；B站除知名UP主外，也有大量高校官方账号发布的完整课程可供补充。");
        selectionTips.add(tip4);
        
        result.put("categories", categories);
        result.put("selection_tips", selectionTips);
        result.put("note", "cover_url字段暂为空，因搜索结果中未提供具体的课程封面图片链接。部分series的url字段为空，因该系列包含多个视频，建议访问UP主主页查看完整合集。");
        
        // 打印默认推荐的详细信息
        log.info("\n=========================================");
        log.info("        默认网课推荐详细信息");
        log.info("=========================================");
        
        if (result.containsKey("categories")) {
            List<Map<String, Object>> categoriesList = (List<Map<String, Object>>) result.get("categories");
            for (int i = 0; i < categoriesList.size(); i++) {
                Map<String, Object> category = categoriesList.get(i);
                log.info("\n【分类 {}】: {}", i + 1, category.get("name"));
                log.info("分类描述: {}", category.get("description"));
                
                if (category.containsKey("courses")) {
                    List<Map<String, Object>> courses = (List<Map<String, Object>>) category.get("courses");
                    for (int j = 0; j < courses.size(); j++) {
                        Map<String, Object> course = courses.get(j);
                        log.info("\n  课程 {}: {}", j + 1, course.get("name"));
                        
                        // 打印课程详细信息
                        if (course.containsKey("instructors")) {
                            log.info("  讲师: {}", course.get("instructors"));
                        }
                        if (course.containsKey("up_name")) {
                            log.info("  UP主: {}", course.get("up_name"));
                        }
                        if (course.containsKey("institution")) {
                            log.info("  机构: {}", course.get("institution"));
                        }
                        if (course.containsKey("platform")) {
                            log.info("  平台: {}", course.get("platform"));
                        }
                        if (course.containsKey("video_url")) {
                            log.info("  视频链接: {}", course.get("video_url"));
                        }
                        if (course.containsKey("features")) {
                            log.info("  课程特点: {}", course.get("features"));
                        }
                        if (course.containsKey("target_audience")) {
                            log.info("  目标受众: {}", course.get("target_audience"));
                        }
                        
                        // 打印B站系列课程
                        if (course.containsKey("series")) {
                            List<Map<String, Object>> series = (List<Map<String, Object>>) course.get("series");
                            log.info("  系列课程:");
                            for (int k = 0; k < series.size(); k++) {
                                Map<String, Object> seriesItem = series.get(k);
                                log.info("    系列 {}: {}", k + 1, seriesItem.get("name"));
                                if (seriesItem.containsKey("url")) {
                                    log.info("    系列链接: {}", seriesItem.get("url"));
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 打印选择建议
        if (result.containsKey("selection_tips")) {
            List<Map<String, Object>> selectionTipsList = (List<Map<String, Object>>) result.get("selection_tips");
            log.info("\n【选择建议】");
            for (int i = 0; i < selectionTipsList.size(); i++) {
                Map<String, Object> tip = selectionTipsList.get(i);
                log.info("  场景 {}: {}", i + 1, tip.get("scenario"));
                log.info("  建议: {}", tip.get("suggestion"));
            }
        }
        
        // 打印备注信息
        if (result.containsKey("note")) {
            log.info("\n【备注】");
            log.info("  {}", result.get("note"));
        }
        
        log.info("=========================================");
        log.info("        默认推荐信息打印完成");
        log.info("=========================================\n");
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getExamQaHistory(Long userId) {
        try {
            if (userId == null) {
                log.warn("userId为null，返回空历史记录");
                return new ArrayList<>();
            }
            
            log.info("用户ID {} 查询考试准备Q&A历史记录", userId);
            
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.error("用户ID {} 不存在", userId);
                return new ArrayList<>();
            }

            List<ExamQaHistory> histories = new ArrayList<>();
            
            // 总是从数据库获取最新数据，确保数据一致性
            histories = examQaHistoryMapper.findByUserId(user.getId());
            log.info("从数据库获取到 {} 条考试准备Q&A历史记录", histories != null ? histories.size() : 0);
            
            try {
                // 根据数据库结果更新Redis缓存
                String userHistoryListKey = "exam:qa:user:history:" + user.getId();
                
                if (histories != null && !histories.isEmpty()) {
                    // 先清空Redis中的历史记录列表，避免缓存脏数据
                    redisTemplate.delete(userHistoryListKey);
                    
                    for (ExamQaHistory history : histories) {
                        String historyKey = "exam:qa:history:" + user.getId() + ":" + history.getId();
                        String answerKey = "exam:qa:answer:" + user.getId() + ":" + history.getId();
                        
                        redisTemplate.opsForValue().set(historyKey, history, java.time.Duration.ofHours(24));
                        redisTemplate.opsForValue().set(answerKey, history.getAnswer(), java.time.Duration.ofHours(24));
                        redisTemplate.opsForList().leftPush(userHistoryListKey, history.getId());
                    }
                    
                    // 限制用户历史记录列表长度
                    redisTemplate.opsForList().trim(userHistoryListKey, 0, 99);
                    
                    log.info("考试准备Q&A历史记录已缓存到Redis");
                } else {
                    // 数据库为空时，清空Redis缓存
                    // 从Redis中获取所有历史记录ID
                    List<Object> historyIds = redisTemplate.opsForList().range(userHistoryListKey, 0, -1);
                    // 删除所有历史记录缓存
                    for (Object id : historyIds) {
                        String historyKey = "exam:qa:history:" + user.getId() + ":" + id;
                        String answerKey = "exam:qa:answer:" + user.getId() + ":" + id;
                        redisTemplate.delete(historyKey);
                        redisTemplate.delete(answerKey);
                    }
                    // 删除历史记录列表
                    redisTemplate.delete(userHistoryListKey);
                    
                    log.info("Redis缓存已清空，因为数据库中没有考试准备Q&A历史记录");
                }
            } catch (Exception e) {
                log.error("更新Redis缓存失败: {}", e.getMessage());
                // Redis操作失败不影响返回结果
            }
            
            // 按创建时间降序排序（最新的在前）
            List<ExamQaHistory> historyList = histories != null ? histories : new ArrayList<>();
            return historyList.stream()
                    .sorted(Comparator.comparing(ExamQaHistory::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(this::convertExamQaToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取考试准备Q&A历史记录失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteExamQaHistory(Long id) {
        try {
            log.info("删除考试准备Q&A历史记录，ID: {}", id);
            
            // 先获取历史记录，以获取userId
            ExamQaHistory history = examQaHistoryMapper.selectById(id);
            if (history != null) {
                Long userId = history.getUserId();
                
                // 从Redis中删除缓存
                try {
                    String userHistoryListKey = "exam:qa:user:history:" + userId;
                    String historyKey = "exam:qa:history:" + userId + ":" + id;
                    String answerKey = "exam:qa:answer:" + userId + ":" + id;
                    
                    // 从用户历史记录列表中移除该记录ID
                    redisTemplate.opsForList().remove(userHistoryListKey, 0, id);
                    // 删除历史记录缓存
                    redisTemplate.delete(historyKey);
                    // 删除回答缓存
                    redisTemplate.delete(answerKey);
                    
                    log.info("Redis缓存删除成功，考试准备Q&A历史记录ID: {}", id);
                } catch (Exception e) {
                    log.error("删除Redis缓存失败: {}", e.getMessage());
                    // Redis操作失败不影响主流程
                }
            }
            
            // 从数据库中删除记录
            examQaHistoryMapper.deleteById(id);
            log.info("考试准备Q&A历史记录删除成功，ID: {}", id);
        } catch (Exception e) {
            log.error("删除考试准备Q&A历史记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除考试准备Q&A历史记录失败，请稍后重试");
        }
    }

    @Override
    public void saveExamQaHistory(String question, String answer, String subject, Long userId) {
        try {
            if (userId == null) {
                log.warn("userId为null，无法保存历史记录");
                throw new RuntimeException("用户未登录");
            }
            
            log.info("用户ID {} 保存考试准备Q&A历史记录", userId);
            
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.error("用户ID {} 不存在", userId);
                throw new RuntimeException("用户不存在");
            }
            
            // 对输入进行安全处理，防止XSS攻击
            String sanitizedQuestion = sanitizeInput(question);
            String sanitizedAnswer = sanitizeInput(answer);
            String sanitizedSubject = sanitizeInput(subject);
            
            ExamQaHistory history = new ExamQaHistory();
            history.setUserId(user.getId());
            history.setSubject(sanitizedSubject);
            history.setQuestionType("EXAM");
            history.setQuestion(sanitizedQuestion);
            history.setAnswer(sanitizedAnswer);
            history.setContext(sanitizedSubject);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());
            
            examQaHistoryMapper.insert(history);
            log.info("考试准备Q&A历史记录保存成功，ID: {}", history.getId());
            
            // Redis缓存处理
            String historyKey = "exam:qa:history:" + user.getId() + ":" + history.getId();
            String answerKey = "exam:qa:answer:" + user.getId() + ":" + history.getId();
            String userHistoryListKey = "exam:qa:user:history:" + user.getId();
            
            // 缓存历史记录
            redisTemplate.opsForValue().set(historyKey, history, java.time.Duration.ofHours(24));
            
            // 缓存回答
            redisTemplate.opsForValue().set(answerKey, sanitizedAnswer, java.time.Duration.ofHours(24));
            
            // 添加到用户历史记录列表
            redisTemplate.opsForList().leftPush(userHistoryListKey, history.getId());
            
            // 限制用户历史记录列表长度（保留最近100条）
            redisTemplate.opsForList().trim(userHistoryListKey, 0, 99);
            
            log.info("Redis缓存成功，考试准备Q&A历史记录ID: {}", history.getId());
        } catch (Exception e) {
            log.error("保存考试准备Q&A历史记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存考试准备Q&A历史记录失败: " + e.getMessage());
        }
    }

    @Override
    public void saveExamQaHistory(String question, String answer, String subject, Long userId, String sessionId) {
        try {
            if (userId == null) {
                log.warn("userId为null，无法保存历史记录");
                throw new RuntimeException("用户未登录");
            }
            
            log.info("用户ID {} 保存考试准备Q&A历史记录，会话ID: {}", userId, sessionId);
            
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.error("用户ID {} 不存在", userId);
                throw new RuntimeException("用户不存在");
            }
            
            String sanitizedQuestion = sanitizeInput(question);
            String sanitizedAnswer = sanitizeInput(answer);
            String sanitizedSubject = sanitizeInput(subject);
            
            ExamQaHistory history = new ExamQaHistory();
            history.setUserId(user.getId());
            history.setSessionId(sessionId);
            history.setSubject(sanitizedSubject);
            history.setQuestionType("EXAM");
            history.setQuestion(sanitizedQuestion);
            history.setAnswer(sanitizedAnswer);
            history.setContext(sanitizedSubject);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());
            
            examQaHistoryMapper.insert(history);
            log.info("考试准备Q&A历史记录保存成功，ID: {}, 会话ID: {}", history.getId(), sessionId);
            
            String historyKey = "exam:qa:history:" + user.getId() + ":" + history.getId();
            String answerKey = "exam:qa:answer:" + user.getId() + ":" + history.getId();
            String userHistoryListKey = "exam:qa:user:history:" + user.getId();
            
            redisTemplate.opsForValue().set(historyKey, history, java.time.Duration.ofHours(24));
            redisTemplate.opsForValue().set(answerKey, sanitizedAnswer, java.time.Duration.ofHours(24));
            redisTemplate.opsForList().leftPush(userHistoryListKey, history.getId());
            redisTemplate.opsForList().trim(userHistoryListKey, 0, 99);
            
            log.info("Redis缓存成功，考试准备Q&A历史记录ID: {}", history.getId());
        } catch (Exception e) {
            log.error("保存考试准备Q&A历史记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存考试准备Q&A历史记录失败: " + e.getMessage());
        }
    }

    private Map<String, Object> convertExamQaToResponse(ExamQaHistory history) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", history.getId());
        response.put("question", history.getQuestion());
        response.put("answer", history.getAnswer());
        response.put("subject", history.getSubject());
        response.put("createdAt", history.getCreatedAt());
        return response;
    }

}


