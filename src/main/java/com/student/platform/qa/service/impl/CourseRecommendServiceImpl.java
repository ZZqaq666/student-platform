package com.student.platform.qa.service.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.qa.entity.QaHistory;
import com.student.platform.qa.entity.ExamQaHistory;
import com.student.platform.qa.mapper.QaHistoryMapper;
import com.student.platform.qa.mapper.ExamQaHistoryMapper;
import com.student.platform.qa.service.AiService;
import com.student.platform.qa.service.CourseRecommendService;
import com.student.platform.qa.service.util.BilibiliVideoSearchService;
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
    private final BilibiliVideoSearchService bilibiliVideoSearchService;

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
     * 基于知识点获取课程推荐（真实联网搜索 + AI 精选）
     *
     * 流程：
     * 1. 对每个知识点真实调用 B站搜索接口，取得真实视频候选（真实 BV 号/标题/UP主/播放量）
     * 2. 让 AI 仅从真实候选中挑选最相关的视频并撰写推荐理由（禁止编造链接）
     * 3. AI 失败时直接返回搜索结果前列（链接依然真实）
     * 4. 后置校验：任何不在候选集合中的链接强制替换为该主题的B站搜索页链接（永远真实有效）
     *
     * @param knowledgePoints 知识点列表
     * @param limit 推荐数量限制
     * @return 课程推荐列表
     */
    private List<Map<String, Object>> getCoursesFromAI(List<String> knowledgePoints, Integer limit) {
        String knowledgePointsStr = String.join("、", knowledgePoints);

        // 第一步：联网真实搜索视频候选
        List<Map<String, Object>> candidates =
                bilibiliVideoSearchService.searchByKeywords(knowledgePoints, 4);
        if (candidates.isEmpty()) {
            log.warn("B站真实搜索无候选结果，知识点: {}", knowledgePointsStr);
            return Collections.emptyList();
        }
        log.info("B站真实搜索获得候选视频 {} 条，知识点: {}", candidates.size(), knowledgePointsStr);

        // 第二步：AI 从真实候选中精选
        List<Map<String, Object>> courses = pickCoursesFromCandidatesWithAI(candidates, knowledgePointsStr, limit);

        // 第三步：后置校验，替换任何编造的链接
        validateAndFixLinks(courses, candidates);

        if (!courses.isEmpty()) {
            for (int i = 0; i < courses.size(); i++) {
                Map<String, Object> course = courses.get(i);
                course.put("id", i + 1);
                if (!course.containsKey("code")) {
                    course.put("code", "#" + UUID.randomUUID().toString().substring(0, 8));
                }
                if (!course.containsKey("platform")) {
                    course.put("platform", "B站");
                }
                if (!course.containsKey("recommendReason")) {
                    course.put("recommendReason", "基于您的知识点从B站真实检索精选");
                }
            }
            log.info("课程推荐完成，共 {} 条（真实B站链接）", courses.size());
            return courses;
        }

        // AI 精选失败：直接返回真实搜索结果前列（链接依然真实）
        log.warn("AI 精选无结果，直接使用B站真实搜索结果");
        return candidates.stream()
                .limit(limit)
                .map(this::toCourseFromCandidate)
                .collect(Collectors.toList());
    }

    /**
     * 让 AI 从真实视频候选中精选推荐
     */
    private List<Map<String, Object>> pickCoursesFromCandidatesWithAI(List<Map<String, Object>> candidates,
                                                                     String knowledgePointsStr, Integer limit) {
        try {
            StringBuilder candidateText = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                Map<String, Object> c = candidates.get(i);
                candidateText.append(String.format("[%d] 标题: %s | UP主: %s | 播放量: %s | 时长: %s | 链接: %s | 搜索关键词: %s%n    简介: %s%n",
                        i + 1, c.get("title"), c.get("author"), c.get("play"), c.get("duration"),
                        c.get("url"), c.get("keyword"), c.get("description")));
            }

            String prompt = "针对知识点「" + knowledgePointsStr + "」，下面是通过B站搜索接口真实检索到的视频候选列表：\n\n"
                    + candidateText
                    + "\n请从上述候选中选出 " + limit + " 个最适合推荐给学生的学习视频，返回JSON数组，字段如下：\n"
                    + "[\n"
                    + "  {\n"
                    + "    \"name\": \"视频标题（可微调，但不得改变视频主题）\",\n"
                    + "    \"desc\": \"课程描述（基于候选的简介提炼）\",\n"
                    + "    \"videoLink\": \"视频链接（必须从候选列表原样复制，一个字都不能改）\",\n"
                    + "    \"platform\": \"B站\",\n"
                    + "    \"recommendReason\": \"推荐理由（结合知识点与UP主权威性、播放量说明）\"\n"
                    + "  }\n"
                    + "]\n"
                    + "硬性要求：\n"
                    + "1. videoLink 只能从候选列表原样复制，严禁编造、拼接或修改任何链接\n"
                    + "2. 优先选择标题与知识点强相关、播放量高、UP主权威（如大学官方账号、名师）的视频\n"
                    + "3. 只输出JSON格式，不要包含其他解释性文本\n";

            String response = aiService.generateResponseWithContext(
                    "你是一个专业的课程推荐助手，只能基于给定的真实视频候选做推荐，绝对不能编造链接",
                    prompt);
            log.info("AI 精选返回的原始响应长度: {}", response == null ? 0 : response.length());

            if (response == null || response.isEmpty()) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> parsed = parseCourseJsonArray(response);
            if (parsed == null || parsed.isEmpty()) {
                return Collections.emptyList();
            }
            return parsed.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("AI 精选候选视频失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 后置校验：不在真实候选集合中的链接一律替换为对应主题的B站搜索页链接
     */
    private void validateAndFixLinks(List<Map<String, Object>> courses, List<Map<String, Object>> candidates) {
        for (Map<String, Object> course : courses) {
            Object linkObj = course.get("videoLink");
            String link = linkObj == null ? "" : String.valueOf(linkObj);
            if (!bilibiliVideoSearchService.isRealCandidateLink(link, candidates)) {
                String name = String.valueOf(course.getOrDefault("name", "大学课程"));
                String fixed = bilibiliVideoSearchService.searchPageUrl(name);
                log.warn("AI 返回的链接不在真实候选中（{}），已替换为B站搜索页链接: {}", link, fixed);
                course.put("videoLink", fixed);
            }
        }
    }

    /**
     * 将真实搜索候选直接转换为课程推荐条目（AI 失败时的兜底）
     */
    private Map<String, Object> toCourseFromCandidate(Map<String, Object> candidate) {
        Map<String, Object> course = new HashMap<>();
        course.put("name", candidate.get("title"));
        course.put("desc", candidate.get("description"));
        course.put("videoLink", candidate.get("url"));
        course.put("platform", "B站");
        course.put("recommendReason", String.format("B站真实检索结果，UP主: %s，播放量: %s",
                candidate.get("author"), candidate.get("play")));
        return course;
    }

    /**
     * 解析 AI 返回的课程 JSON 数组（含容错处理）
     */
    private List<Map<String, Object>> parseCourseJsonArray(String response) {
        try {
            String processedResponse = response;
            // 先移除可能的代码块标记
            processedResponse = processedResponse.replaceAll("^```json", "");
            processedResponse = processedResponse.replaceAll("^```", "");
            processedResponse = processedResponse.replaceAll("```$", "");

            // 移除开头和结尾的反引号
            while (processedResponse.startsWith("`")) {
                processedResponse = processedResponse.substring(1);
            }
            while (processedResponse.endsWith("`")) {
                processedResponse = processedResponse.substring(0, processedResponse.length() - 1);
            }
            processedResponse = processedResponse.trim();

            // 尝试修复JSON格式问题
            String fixedResponse = fixJsonFormat(processedResponse);
            if (!fixedResponse.equals(processedResponse)) {
                processedResponse = fixedResponse;
            }

            // 确保响应以 [ 或 { 开头，否则尝试提取有效的JSON部分
            if (!(processedResponse.startsWith("[") || processedResponse.startsWith("{"))) {
                String extractedJson = extractValidJson(processedResponse);
                if (!extractedJson.isEmpty()) {
                    processedResponse = extractedJson;
                }
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);

            return objectMapper.readValue(processedResponse, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("解析AI返回的课程JSON失败: {}", e.getMessage());
            try {
                String extractedJson = extractValidJson(response);
                if (!extractedJson.isEmpty()) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(extractedJson, new TypeReference<>() {
                    });
                }
            } catch (Exception ex) {
                log.error("提取有效JSON失败: {}", ex.getMessage());
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
