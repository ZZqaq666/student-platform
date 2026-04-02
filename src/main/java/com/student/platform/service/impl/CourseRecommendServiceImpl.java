package com.student.platform.service.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.entity.QaHistory;
import com.student.platform.entity.ExamQaHistory;
import com.student.platform.mapper.QaHistoryMapper;
import com.student.platform.mapper.ExamQaHistoryMapper;
import com.student.platform.service.AiService;
import com.student.platform.service.CourseRecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRecommendServiceImpl implements CourseRecommendService {
    
    private final QaHistoryMapper qaHistoryMapper;
    private final ExamQaHistoryMapper examQaHistoryMapper;
    private final AiService aiService;
    
    @Override
    public List<Map<String, Object>> recommendCoursesByQuestionHistory(Long userId, Integer limit) {
        try {
            log.info("基于用户提问历史推荐课程，用户ID: {}, 限制: {}", userId, limit);
            
            if (userId == null) {
                log.warn("用户ID为空，返回默认推荐");
                return getDefaultRecommendations(limit);
            }
            
            List<QaHistory> histories = qaHistoryMapper.findByUserId(userId);
            if (histories == null || histories.isEmpty()) {
                log.info("用户 {} 没有提问历史，返回默认推荐", userId);
                return getDefaultRecommendations(limit);
            }
            
            List<QaHistory> recentHistories;
            try {
                recentHistories = histories.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                            return 0;
                        }
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .limit(10)
                    .toList();
            } catch (Exception e) {
                log.error("排序历史记录失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
            String questionsText = "";
            try {
                questionsText = recentHistories.stream()
                    .map(QaHistory::getQuestion)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            } catch (Exception e) {
                log.error("收集问题文本失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
            if (questionsText.isEmpty()) {
                return getDefaultRecommendations(limit);
            }
            
            List<String> knowledgePoints;
            try {
                knowledgePoints = extractKnowledgePoints(questionsText);
            } catch (Exception e) {
                log.error("提取知识点失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
            if (knowledgePoints == null || knowledgePoints.isEmpty()) {
                return getDefaultRecommendations(limit);
            }
            
            try {
                return recommendCoursesByKnowledge(knowledgePoints, limit);
            } catch (Exception e) {
                log.error("基于知识点推荐课程失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
        } catch (Exception e) {
            log.error("基于提问历史推荐课程失败: {}", e.getMessage(), e);
            return getDefaultRecommendations(limit);
        }
    }
    
    @Override
    public CompletableFuture<List<Map<String, Object>>> recommendCoursesByQuestionHistoryAsync(Long userId, Integer limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("异步基于用户提问历史推荐课程，用户ID: {}, 限制: {}", userId, limit);
                return recommendCoursesByQuestionHistory(userId, limit);
            } catch (Exception e) {
                log.error("异步推荐课程失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
        });
    }
    
    @Override
    public List<Map<String, Object>> recommendCoursesByKnowledge(List<String> knowledgePoints, Integer limit) {
        try {
            log.info("基于知识点推荐课程，知识点: {}, 限制: {}", knowledgePoints, limit);
            
            if (knowledgePoints == null || knowledgePoints.isEmpty()) {
                log.warn("知识点为空，返回默认推荐");
                return getDefaultRecommendations(limit);
            }
            
            // 调用AI服务获取课程推荐
            List<Map<String, Object>> recommendedCourses;
            try {
                recommendedCourses = getCoursesFromAI(knowledgePoints, limit);
                if (recommendedCourses != null && !recommendedCourses.isEmpty()) {
                    log.info("AI推荐课程成功，数量: {}", recommendedCourses.size());
                    for (int i = 0; i < recommendedCourses.size(); i++) {
                        log.info("AI推荐课程 {}: {} - {}", i + 1, recommendedCourses.get(i).get("name"), recommendedCourses.get(i).get("videoLink"));
                    }
                    return recommendedCourses;
                } else {
                    log.warn("AI返回的课程为空，返回默认推荐");
                    return getDefaultRecommendations(limit);
                }
            } catch (Exception e) {
                log.error("调用AI获取课程推荐失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
        } catch (Exception e) {
            log.error("基于知识点推荐课程失败: {}", e.getMessage(), e);
            return getDefaultRecommendations(limit);
        }
    }
    
    /**
     * 调用AI服务获取课程推荐
     * @param knowledgePoints 知识点列表
     * @param limit 推荐数量限制
     * @return 课程推荐列表
     */
    private List<Map<String, Object>> getCoursesFromAI(List<String> knowledgePoints, Integer limit) {
        String knowledgePointsStr = String.join("、", knowledgePoints);
        String prompt = "请根据以下知识点推荐" + limit + "个相关的优质课程，返回JSON格式，包含以下字段：\n" +
            "[\n" +
            "  {\n" +
            "    \"id\": 课程ID,\n" +
            "    \"name\": \"课程名称\",\n" +
            "    \"desc\": \"课程描述\",\n" +
            "    \"code\": \"课程代码\",\n" +
            "    \"videoLink\": \"视频链接\",\n" +
            "    \"platform\": \"平台名称\",\n" +
            "    \"recommendReason\": \"推荐理由\"\n" +
            "  }\n" +
            "]\n" +
            "要求：\n" +
            "1. 推荐的课程必须来自中国大陆可访问的平台，如B站、中国大学MOOC、智慧树等\n" +
            "2. 课程必须与以下知识点相关：" + knowledgePointsStr + "\n" +
            "3. 直接返回JSON格式，不要包含其他解释性文本\n" +
            "4. 确保JSON格式正确，可直接解析\n" +
            "5. 每个课程的视频链接必须有效且与课程内容相关\n" +
            "6. 推荐理由要具体，说明与知识点的关联性\n";
        
        String response = aiService.generateResponseWithContext("你是一个专业的课程推荐助手，能够根据知识点推荐相关的优质课程", prompt);
        log.info("AI返回的原始响应: '{}'", response);
        
        if (response == null || response.isEmpty()) {
            log.warn("AI返回的响应为空");
            return Collections.emptyList();
        }
        
        // 预处理AI返回的响应，处理可能的格式问题
        String processedResponse = response;
        try {
            // 先移除可能的代码块标记
            processedResponse = processedResponse.replaceAll("^```json", "");
            processedResponse = processedResponse.replaceAll("^```", "");
            processedResponse = processedResponse.replaceAll("```$", "");
            
            // 移除开头和结尾的反引号
            while (processedResponse.startsWith("`")) {
                processedResponse = processedResponse.substring(1);
                log.info("移除开头的反引号");
            }
            while (processedResponse.endsWith("`")) {
                processedResponse = processedResponse.substring(0, processedResponse.length() - 1);
                log.info("移除结尾的反引号");
            }
            
            // 去除首尾空白字符
            processedResponse = processedResponse.trim();
            
            log.info("预处理后的AI响应: '{}'", processedResponse);
            
            // 尝试修复JSON格式问题
            String fixedResponse = fixJsonFormat(processedResponse);
            if (!fixedResponse.equals(processedResponse)) {
                log.info("修复后的JSON响应: '{}'", fixedResponse);
                processedResponse = fixedResponse;
            }
            
            // 再次检查并移除可能的开头反引号
            while (processedResponse.startsWith("`")) {
                processedResponse = processedResponse.substring(1);
                log.info("再次移除开头的反引号");
            }
            
            // 确保响应以 [ 或 { 开头，否则尝试提取有效的JSON部分
            if (!(processedResponse.startsWith("[") || processedResponse.startsWith("{"))) {
                log.info("响应不是有效的JSON开头，尝试提取有效的JSON部分");
                String extractedJson = extractValidJson(processedResponse);
                if (!extractedJson.isEmpty()) {
                    log.info("提取到有效的JSON部分: '{}'", extractedJson);
                    processedResponse = extractedJson;
                }
            }
            
            // 解析AI返回的JSON
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置ObjectMapper以更宽容地处理JSON格式
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
            
            List<Map<String, Object>> courses = objectMapper.readValue(processedResponse, new TypeReference<>() {
            });
            
            // 处理返回的课程数据，确保字段完整
            for (int i = 0; i < courses.size(); i++) {
                Map<String, Object> course = courses.get(i);
                // 确保必要字段存在
                if (!course.containsKey("id")) {
                    course.put("id", i + 1);
                }
                if (!course.containsKey("code")) {
                    course.put("code", "#" + java.util.UUID.randomUUID().toString().substring(0, 8));
                }
                if (!course.containsKey("platform")) {
                    String videoLink = (String) course.get("videoLink");
                    if (videoLink != null) {
                        if (videoLink.contains("bilibili.com")) {
                            course.put("platform", "B站");
                        } else if (videoLink.contains("icourse163.org")) {
                            course.put("platform", "中国大学MOOC");
                        } else if (videoLink.contains("zhihuishu.com")) {
                            course.put("platform", "智慧树");
                        } else {
                            course.put("platform", "国内平台");
                        }
                    } else {
                        course.put("platform", "国内平台");
                    }
                }
                if (!course.containsKey("recommendReason")) {
                    course.put("recommendReason", "基于您的知识点推荐");
                }
            }
            
            return courses;
        } catch (Exception e) {
            log.error("解析AI返回的JSON失败: {}", e.getMessage(), e);
            log.error("AI返回的原始响应: '{}'", response);
            log.error("预处理后的响应: '{}'", processedResponse);
            
            // 尝试使用更简单的方法解析，或者返回默认推荐
            try {
                // 尝试提取有效的JSON部分
                String extractedJson = extractValidJson(processedResponse);
                if (!extractedJson.isEmpty()) {
                    log.info("尝试解析提取的JSON: '{}'", extractedJson);
                    ObjectMapper objectMapper = new ObjectMapper();
                    List<Map<String, Object>> courses = objectMapper.readValue(extractedJson, new TypeReference<>() {
                    });
                    return courses;
                }
            } catch (Exception ex) {
                log.error("尝试提取有效JSON失败: {}", ex.getMessage());
            }
            
            return Collections.emptyList();
        }
    }
    
    /**
     * 修复JSON格式问题
     * @param json JSON字符串
     * @return 修复后的JSON字符串
     */
    private String fixJsonFormat(String json) {
        // 尝试修复常见的JSON格式问题
        String fixed = json;
        
        // 修复未闭合的引号
        int quoteCount = 0;
        for (int i = 0; i < fixed.length(); i++) {
            if (fixed.charAt(i) == '"') {
                quoteCount++;
            }
        }
        
        // 如果引号数量为奇数，尝试在末尾添加引号
        if (quoteCount % 2 != 0) {
            fixed = fixed + '"';
            log.info("修复了未闭合的引号");
        }
        
        // 修复未闭合的括号
        int openBrackets = 0;
        int closeBrackets = 0;
        int openBraces = 0;
        int closeBraces = 0;
        
        for (int i = 0; i < fixed.length(); i++) {
            char c = fixed.charAt(i);
            if (c == '[') openBrackets++;
            else if (c == ']') closeBrackets++;
            else if (c == '{') openBraces++;
            else if (c == '}') closeBraces++;
        }
        
        // 修复未闭合的方括号
        while (openBrackets > closeBrackets) {
            fixed = fixed + ']';
            closeBrackets++;
            log.info("修复了未闭合的方括号");
        }
        
        // 修复未闭合的花括号
        while (openBraces > closeBraces) {
            fixed = fixed + '}';
            closeBraces++;
            log.info("修复了未闭合的花括号");
        }
        
        return fixed;
    }
    
    /**
     * 提取有效的JSON部分
     * @param text 包含JSON的文本
     * @return 提取的有效JSON部分
     */
    private String extractValidJson(String text) {
        // 找到JSON数组的开始和结束位置
        int startIndex = text.indexOf('[');
        int endIndex = text.lastIndexOf(']');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex + 1);
        }
        
        // 尝试找到JSON对象的开始和结束位置
        startIndex = text.indexOf('{');
        endIndex = text.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex + 1);
        }
        
        return "";
    }
    
    @Override
    public List<Map<String, Object>> recommendCoursesBySubject(String subject, Integer limit) {
        try {
            log.info("基于学科推荐课程，学科: {}, 限制: {}", subject, limit);
            
            if (subject == null || subject.isEmpty()) {
                return getDefaultRecommendations(limit);
            }
            
            // 调用AI服务获取课程推荐
            List<Map<String, Object>> recommendedCourses;
            try {
                recommendedCourses = getCoursesFromAI(java.util.Collections.singletonList(subject), limit);
                if (recommendedCourses != null && !recommendedCourses.isEmpty()) {
                    log.info("AI推荐课程成功，数量: {}", recommendedCourses.size());
                    for (int i = 0; i < recommendedCourses.size(); i++) {
                        log.info("AI推荐课程 {}: {} - {}", i + 1, recommendedCourses.get(i).get("name"), recommendedCourses.get(i).get("videoLink"));
                    }
                    return recommendedCourses;
                } else {
                    log.warn("AI返回的课程为空，返回默认推荐");
                    return getDefaultRecommendations(limit);
                }
            } catch (Exception e) {
                log.error("调用AI获取课程推荐失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
        } catch (Exception e) {
            log.error("基于学科推荐课程失败: {}", e.getMessage(), e);
            return getDefaultRecommendations(limit);
        }
    }

    @Override
    public List<Map<String, Object>> recommendCoursesByExamQaHistory(Long userId, Integer limit) {
        try {
            log.info("基于用户考试准备Q&A历史推荐课程，用户ID: {}, 限制: {}", userId, limit);
            
            if (userId == null) {
                log.warn("用户ID为空，返回默认推荐");
                return getDefaultRecommendations(limit);
            }
            
            List<ExamQaHistory> histories = examQaHistoryMapper.findByUserId(userId);
            if (histories == null || histories.isEmpty()) {
                log.info("用户 {} 没有考试准备Q&A历史，返回默认推荐", userId);
                return getDefaultRecommendations(limit);
            }
            
            List<ExamQaHistory> recentHistories;
            try {
                recentHistories = histories.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                            return 0;
                        }
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .limit(10)
                    .toList();
            } catch (Exception e) {
                log.error("排序考试准备Q&A历史记录失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
            String questionsText = "";
            try {
                questionsText = recentHistories.stream()
                    .map(ExamQaHistory::getQuestion)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            } catch (Exception e) {
                log.error("收集考试准备Q&A问题文本失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
            if (questionsText.isEmpty()) {
                return getDefaultRecommendations(limit);
            }
            
            List<String> knowledgePoints;
            try {
                knowledgePoints = extractKnowledgePoints(questionsText);
            } catch (Exception e) {
                log.error("提取知识点失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
            if (knowledgePoints == null || knowledgePoints.isEmpty()) {
                return getDefaultRecommendations(limit);
            }
            
            try {
                return recommendCoursesByKnowledge(knowledgePoints, limit);
            } catch (Exception e) {
                log.error("基于知识点推荐课程失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
            
        } catch (Exception e) {
            log.error("基于考试准备Q&A历史推荐课程失败: {}", e.getMessage(), e);
            return getDefaultRecommendations(limit);
        }
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> recommendCoursesByExamQaHistoryAsync(Long userId, Integer limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("异步基于用户考试准备Q&A历史推荐课程，用户ID: {}, 限制: {}", userId, limit);
                return recommendCoursesByExamQaHistory(userId, limit);
            } catch (Exception e) {
                log.error("异步推荐课程失败: {}", e.getMessage(), e);
                return getDefaultRecommendations(limit);
            }
        });
    }
    
    private List<String> extractKnowledgePoints(String questionsText) {
        try {
            String prompt = "请从以下问题中提取主要知识点（最多5个），用逗号分隔，只返回知识点名称：\n" + questionsText;
            String response = aiService.generateResponseWithContext("你是一个知识点提取助手，专门从问题中提取核心知识点", prompt);
            
            log.info("AI 响应原始内容: '{}'", response);
            log.info("AI 响应长度: {}", response != null ? response.length() : 0);
            
            if (response == null || response.isEmpty()) {
                log.warn("AI 响应为空");
                return Collections.emptyList();
            }
            
            String[] parts = response.split("[,，、]");
            log.info("分割后的知识点数量: {}", parts.length);
            for (int i = 0; i < parts.length; i++) {
                log.info("知识点 {}: '{}'", i + 1, parts[i].trim());
            }
            
            List<String> knowledgePoints = Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(5)
                .collect(Collectors.toList());
            
            log.info("最终提取的知识点: {}", knowledgePoints);
            return knowledgePoints;
            
        } catch (Exception e) {
            log.error("提取知识点失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    

    
    private List<Map<String, Object>> getDefaultRecommendations(Integer limit) {
        List<Map<String, Object>> defaultCourses = new ArrayList<>();

        String[][] defaultData = {
                {"1", "高等数学精讲", "涵盖微积分、线性代数等核心内容", "#36CFCC", "https://www.bilibili.com/video/BV1Eb411u7Fw"}, // 宋浩老师同济版高数全程
                {"2", "线性代数入门", "矩阵、行列式、特征值详解", "#4CAF50", "https://www.bilibili.com/video/BV1aW411Q7x1"}, // 宋浩老师线性代数
                {"3", "概率论与数理统计", "概率分布、统计推断基础", "#FF9800", "https://www.bilibili.com/video/BV1ot411y7mU"}, // 宋浩老师概率论
                {"4", "大学物理", "力学、电磁学、光学综合", "#2196F3", "https://open.163.com/newview/movie/courseintro?newurl=PGV82JH1H"}, // 网易公开课：大学物理（完整涵盖力学+电磁学+光学）
                {"5", "数据结构与算法", "常见数据结构与经典算法", "#9C27B0", "https://www.bilibili.com/video/BV1H4411N7oD"}  // 浙江大学陈越、何钦铭教授最新版
        };
        
        for (int i = 0; i < Math.min(limit, defaultData.length); i++) {
            Map<String, Object> course = new HashMap<>();
            course.put("id", Long.parseLong(defaultData[i][0]));
            course.put("name", defaultData[i][1]);
            course.put("desc", defaultData[i][2]);
            course.put("code", defaultData[i][3]);
            course.put("videoLink", defaultData[i][4]);
            course.put("recommendReason", "热门推荐");
            defaultCourses.add(course);
        }
        
        return defaultCourses;
    }
}
