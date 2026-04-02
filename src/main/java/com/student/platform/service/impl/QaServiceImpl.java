package com.student.platform.service.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.dto.ExamPaper;
import com.student.platform.dto.ExamPaperResponse;
import com.student.platform.dto.Question;
import com.student.platform.dto.QaResponse;
import com.student.platform.entity.*;
import com.student.platform.mapper.*;
import com.student.platform.service.AiService;
import com.student.platform.service.QaService;
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
    private final BookMapper bookMapper;
    private final BookChapterMapper bookChapterMapper;
    private final CourseMapper courseMapper;
    private final ExamSubjectMapper examSubjectMapper;
    private final ExamKeyPointMapper examKeyPointMapper;
    private final ExamHistoryMapper examHistoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserProfileMapper userProfileMapper;
    private final UserSubjectPreferenceMapper userSubjectPreferenceMapper;
    private final UserWeakKnowledgeMapper userWeakKnowledgeMapper;
    private final AiService aiService;

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
                history.getQuestion(),
                history.getAnswer(),
                history.getContext(),
                history.getBookId(),
                history.getKnowledgeNodeId(),
                history.getCreatedAt()
        );
    }

    public List<Map<String, Object>> getExamSubjects() {
        try {
            log.info("获取考试科目");
            List<ExamSubject> examSubjects = examSubjectMapper.selectList(null);
            List<Map<String, Object>> subjects = examSubjects.stream()
                .map(subject -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", subject.getId());
                    map.put("name", subject.getName());
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("获取到 {} 个考试科目", subjects.size());
            return subjects;
        } catch (Exception e) {
            log.error("获取考试科目失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取考试科目失败，请稍后重试");
        }
    }

    public List<Map<String, Object>> getKeyPoints(String subjectId) {
        try {
            log.info("获取知识点，科目ID: {}", subjectId);
            List<ExamKeyPoint> examKeyPoints = examKeyPointMapper.selectBySubjectId(subjectId);

            Map<String, Object> subjectKeyPoints = new HashMap<>();

            // 获取科目名称
            ExamSubject subject = examSubjectMapper.selectById(subjectId);
            String subjectName = subject != null ? subject.getName() : "未知科目";

            List<String> keyPointNames = examKeyPoints.stream()
                .map(ExamKeyPoint::getName)
                .collect(Collectors.toList());

            subjectKeyPoints.put("subject", subjectName);
            subjectKeyPoints.put("keyPoints", keyPointNames);

            List<Map<String, Object>> keyPoints = new ArrayList<>();
            keyPoints.add(subjectKeyPoints);

            log.info("获取到 {} 个知识点", keyPointNames.size());
            return keyPoints;
        } catch (Exception e) {
            log.error("获取知识点失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取知识点失败，请稍后重试");
        }
    }

    @Override
    public List<Map<String, Object>> getExamPapers(String subjectId) {
        try {
            log.info("获取真题试卷，科目ID: {}", subjectId);

            // 获取科目名称
            ExamSubject subject = examSubjectMapper.selectById(subjectId);
            String subjectName = subject != null ? subject.getName() : "未知科目";

            // 根据科目返回对应的真题数据
            List<Map<String, Object>> papers = new ArrayList<>();

            // 根据科目ID返回不同的真题数据
            switch (subjectId) {
                case "math":
                    papers.add(createPaper("2024", "高等数学（上）期末考试", "math", subjectName, 120, 100));
                    papers.add(createPaper("2024", "高等数学（下）期末考试", "math", subjectName, 120, 100));
                    papers.add(createPaper("2023", "高等数学期中考试", "math", subjectName, 90, 100));
                    papers.add(createPaper("2023", "高等数学期末考试", "math", subjectName, 120, 100));
                    break;
                case "english":
                    papers.add(createPaper("2024", "大学英语四级模拟", "english", subjectName, 125, 710));
                    papers.add(createPaper("2024", "大学英语期末测试", "english", subjectName, 120, 100));
                    papers.add(createPaper("2023", "英语六级真题", "english", subjectName, 130, 710));
                    break;
                case "physics":
                    papers.add(createPaper("2024", "大学物理（力学）期末", "physics", subjectName, 120, 100));
                    papers.add(createPaper("2024", "大学物理（电磁学）期末", "physics", subjectName, 120, 100));
                    papers.add(createPaper("2023", "普通物理综合测试", "physics", subjectName, 150, 100));
                    break;
                case "cs":
                    papers.add(createPaper("2024", "数据结构与算法期末", "cs", subjectName, 120, 100));
                    papers.add(createPaper("2024", "计算机组成原理测试", "cs", subjectName, 90, 100));
                    papers.add(createPaper("2023", "操作系统期末考试", "cs", subjectName, 120, 100));
                    break;
                case "builder":
                    papers.add(createPaper("2024", "建筑力学基础测试", "builder", subjectName, 90, 100));
                    papers.add(createPaper("2024", "建筑材料与构造期末", "builder", subjectName, 120, 100));
                    papers.add(createPaper("2023", "建筑设计原理考试", "builder", subjectName, 180, 100));
                    break;
                default:
                    // 默认返回一些通用试卷
                    papers.add(createPaper("2024", "综合测试卷A", subjectId, subjectName, 120, 100));
                    papers.add(createPaper("2024", "综合测试卷B", subjectId, subjectName, 120, 100));
                    papers.add(createPaper("2023", "模拟考试卷", subjectId, subjectName, 90, 100));
                    break;
            }

            log.info("获取到 {} 份真题试卷", papers.size());
            return papers;
        } catch (Exception e) {
            log.error("获取真题试卷失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取真题试卷失败，请稍后重试");
        }
    }

    @Override
    public List<Map<String, Object>> generateExamPapersByAI(String subjectId) {
        try {
            log.info("AI生成真题推荐，科目ID: {}", subjectId);

            // 获取科目名称
            ExamSubject subject = examSubjectMapper.selectById(subjectId);
            String subjectName = subject != null ? subject.getName() : "未知科目";

            // 生成缓存键，基于科目ID
            String cacheKey = "exam:papers:ai:" + subjectId;

            // 尝试从Redis获取缓存
            try {
                Object cachedResult = redisTemplate.opsForValue().get(cacheKey);
                if (cachedResult != null) {
                    log.info("从Redis缓存获取真题推荐数据，科目ID: {}", subjectId);
                    return (List<Map<String, Object>>) cachedResult;
                }
            } catch (Exception e) {
                log.warn("从Redis获取缓存失败: {}", e.getMessage());
            }

            // 构建AI提示词
            String jsonPrompt = "基于以下科目，生成3-5份真题试卷推荐，每份试卷必须包含完整的题目列表，返回JSON格式数据：\n" +
                    "科目：" + subjectName + "\n" +
                    "\n" +
                    "要求：\n" +
                    "1. 必须返回一个包含examPapers数组的JSON对象，每个试卷包含完整的题目列表，格式如下：\n" +
                    "{\n" +
                    "  \"examPapers\": [\n" +
                    "    {\n" +
                    "      \"id\": \"唯一标识符\",\n" +
                    "      \"year\": \"年份（如2024）\",\n" +
                    "      \"paperName\": \"试卷名称\",\n" +
                    "      \"subjectId\": \"" + subjectId + "\",\n" +
                    "      \"subjectName\": \"" + subjectName + "\",\n" +
                    "      \"duration\": 考试时长（分钟）,\n" +
                    "      \"totalScore\": 总分,\n" +
                    "      \"questionCount\": 题目数量（如80）,\n" +
                    "      \"difficulty\": 难度等级（1-5，1最简单，5最难）,\n" +
                    "      \"recommendReason\": \"推荐理由\",\n" +
                    "      \"source\": \"来源\",\n" +
                    "      \"questions\": [\n" +
                    "        {\n" +
                    "          \"id\": \"题目唯一标识\",\n" +
                    "          \"type\": \"选择题\",\n" +
                    "          \"content\": \"题目内容\",\n" +
                    "          \"options\": [\"A. 选项1\", \"B. 选项2\", \"C. 选项3\", \"D. 选项4\"],\n" +
                    "          \"correctAnswer\": 0,\n" +
                    "          \"analysis\": \"答案解析\",\n" +
                    "          \"score\": 分值\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n" +
                    "2. 每个试卷必须包含所有上述字段，不能为空\n" +
                    "3. 每个试卷的questions数组必须包含与questionCount数量相等的题目\n" +
                    "4. 题目类型包括：选择题、填空题、解答题，以选择题为主\n" +
                    "5. 每道题必须包含：id、type、content、options（选择题）、correctAnswer、analysis、score\n" +
                    "6. 直接返回JSON对象，不要包含其他解释性文本\n" +
                    "7. 确保JSON格式正确，可直接被解析\n" +
                    "8. 推荐的真题应与该科目相关，具有实际参考价值\n" +
                    "9. 确保返回完整的试卷列表和完整的题目列表，不要遗漏任何数据";

            // 调用AI服务生成推荐
            String aiResponse = aiService.generateResponseWithContext("", jsonPrompt);
            aiResponse = preprocessAiResponse(aiResponse);

            log.info("AI响应预处理后的内容: {}", aiResponse);
            log.info("AI响应长度: {}", aiResponse.length());

            // 解析AI返回的JSON
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> papers = new ArrayList<>();

            try {
                // 尝试使用结构化DTO解析
                try {
                    ExamPaperResponse response = objectMapper.readValue(aiResponse, ExamPaperResponse.class);
                    List<ExamPaper> examPapers = response.getExamPapers();
                    
                    if (examPapers != null && !examPapers.isEmpty()) {
                        log.info("成功解析为ExamPaperResponse，包含 {} 份试卷", examPapers.size());
                        
                        // 转换为Map格式
                        for (ExamPaper examPaper : examPapers) {
                            Map<String, Object> paperMap = new HashMap<>();
                            paperMap.put("id", examPaper.getId() != null ? examPaper.getId() : subjectId + "-ai-" + examPapers.indexOf(examPaper));
                            // 使用固定的ID格式，避免时间戳导致的ID不匹配
                            paperMap.put("year", examPaper.getYear() != null ? examPaper.getYear() : "2024");
                            paperMap.put("paperName", examPaper.getPaperName() != null ? examPaper.getPaperName() : "AI推荐真题");
                            paperMap.put("subjectId", examPaper.getSubjectId() != null ? examPaper.getSubjectId() : subjectId);
                            paperMap.put("subjectName", examPaper.getSubjectName() != null ? examPaper.getSubjectName() : subjectName);
                            paperMap.put("duration", examPaper.getDuration() > 0 ? examPaper.getDuration() : 120);
                            paperMap.put("totalScore", examPaper.getTotalScore() > 0 ? examPaper.getTotalScore() : 100);
                            paperMap.put("questionCount", examPaper.getQuestionCount() > 0 ? examPaper.getQuestionCount() : 25);
                            paperMap.put("difficulty", examPaper.getDifficulty() > 0 ? examPaper.getDifficulty() : 3);
                            paperMap.put("recommendReason", examPaper.getRecommendReason() != null ? examPaper.getRecommendReason() : "AI推荐的优质真题");
                            paperMap.put("source", examPaper.getSource() != null ? examPaper.getSource() : "AI生成");
                            
                            // 处理题目列表
                            List<Question> questionsList = examPaper.getQuestions();
                            List<Map<String, Object>> questions = new ArrayList<>();
                            int questionCount = (int) paperMap.get("questionCount");
                            
                            if (questionsList != null && !questionsList.isEmpty()) {
                                for (int i = 0; i < questionsList.size(); i++) {
                                    Question q = questionsList.get(i);
                                    Map<String, Object> questionMap = new HashMap<>();
                                    questionMap.put("id", q.getId() != null ? q.getId() : "q-" + i);
                                    questionMap.put("type", q.getType() != null ? q.getType() : "选择题");
                                    questionMap.put("content", q.getContent() != null ? q.getContent() : "第" + (i + 1) + "题");
                                    questionMap.put("question", q.getQuestion() != null ? q.getQuestion() : (q.getContent() != null ? q.getContent() : "第" + (i + 1) + "题"));
                                    questionMap.put("options", q.getOptions() != null ? q.getOptions() : Arrays.asList("A. 选项A", "B. 选项B", "C. 选项C", "D. 选项D"));
                                    questionMap.put("correctAnswer", q.getCorrectAnswer() != null ? q.getCorrectAnswer() : 0);
                                    questionMap.put("analysis", q.getAnalysis() != null ? q.getAnalysis() : "本题考查" + subjectName + "的基础知识。");
                                    questionMap.put("score", q.getScore() > 0 ? q.getScore() : (int) paperMap.get("totalScore") / questionCount);
                                    questions.add(questionMap);
                                }
                                
                                // 使用实际的题目数量，不补充默认题目
                                // 更新questionCount为实际的题目数量
                                paperMap.put("questionCount", questions.size());
                                log.info("试卷 {} 实际题目数量: {}，已更新questionCount", examPaper.getPaperName(), questions.size());
                            } else {
                                // 生成默认题目
                                questions = generateDefaultQuestions(questionCount, subjectName);
                            }
                            
                            paperMap.put("questions", questions);
                            paperMap.put("createTime", LocalDateTime.now());
                            papers.add(paperMap);
                        }
                        
                        log.info("AI生成真题推荐成功，获取到 {} 份真题", papers.size());
                    } else {
                        // 如果没有试卷列表，尝试提取部分数据
                        log.warn("AI返回的响应不包含试卷列表，尝试提取部分数据");
                        papers = extractPartialExamData(aiResponse, subjectId, subjectName);
                    }
                } catch (Exception e) {
                    log.warn("使用DTO解析失败，尝试传统解析方式: {}", e.getMessage());
                    
                    // 传统解析方式
                    // 先检测JSON类型
                    JsonParser parser = objectMapper.getFactory().createParser(aiResponse);
                    JsonToken token = parser.nextToken();
                    parser.close();

                    log.info("检测到的JSON类型: {}", token);

                    // 尝试解析JSON对象（优先）
                    try {
                        Map<String, Object> jsonObject = objectMapper.readValue(aiResponse, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                        
                        log.info("成功解析为JSON对象，包含字段: {}", jsonObject.keySet());
                        
                        // 检查对象中是否包含试卷列表
                        if (jsonObject.containsKey("examPapers")) {
                            Object papersObject = jsonObject.get("examPapers");
                            
                            log.info("examPapers字段类型: {}", papersObject != null ? papersObject.getClass().getName() : "null");
                            
                            if (papersObject instanceof List) {
                                papers = (List<Map<String, Object>>) papersObject;
                                
                                // 验证并处理数据
                                for (Map<String, Object> paper : papers) {
                                    // 确保所有必要字段存在
                                    if (!paper.containsKey("id")) {
                                        paper.put("id", subjectId + "-ai-" + papers.indexOf(paper));
                                        // 使用固定的ID格式，避免时间戳导致的ID不匹配
                                    }
                                    if (!paper.containsKey("year")) {
                                        paper.put("year", "2024");
                                    }
                                    if (!paper.containsKey("paperName")) {
                                        paper.put("paperName", "AI推荐真题");
                                    }
                                    if (!paper.containsKey("subjectId")) {
                                        paper.put("subjectId", subjectId);
                                    }
                                    if (!paper.containsKey("subjectName")) {
                                        paper.put("subjectName", subjectName);
                                    }
                                    if (!paper.containsKey("duration")) {
                                        paper.put("duration", 120);
                                    }
                                    if (!paper.containsKey("totalScore")) {
                                        paper.put("totalScore", 100);
                                    }
                                    if (!paper.containsKey("questionCount")) {
                                        paper.put("questionCount", 25);
                                    }
                                    if (!paper.containsKey("difficulty")) {
                                        paper.put("difficulty", 3);
                                    }
                                    if (!paper.containsKey("recommendReason")) {
                                        paper.put("recommendReason", "AI推荐的优质真题");
                                    }
                                    if (!paper.containsKey("source")) {
                                        paper.put("source", "AI生成");
                                    }
                                    
                                    // 处理题目列表
                                    int questionCount = (int) paper.getOrDefault("questionCount", 25);
                                    List<Map<String, Object>> questions = new ArrayList<>();
                                    
                                    if (!paper.containsKey("questions")) {
                                        // 如果没有题目列表，生成默认题目
                                        log.warn("试卷 {} 没有题目列表，生成默认题目", paper.get("paperName"));
                                        questions = generateDefaultQuestions(questionCount, subjectName);
                                    } else {
                                        Object questionsObj = paper.get("questions");
                                        
                                        if (questionsObj instanceof List) {
                                            // 处理题目列表
                                            questions = (List<Map<String, Object>>) questionsObj;
                                            // 使用实际的题目数量，不补充默认题目
                                            log.info("试卷 {} 实际题目数量: {}", paper.get("paperName"), questions.size());
                                        } else if (questionsObj instanceof Map) {
                                            // 处理题目对象
                                            Map<String, Object> questionsMap = (Map<String, Object>) questionsObj;
                                            
                                            // 检查是否有"question"字段（可能是单个问题或问题列表）
                                            if (questionsMap.containsKey("question")) {
                                                Object questionObj = questionsMap.get("question");
                                                if (questionObj instanceof List) {
                                                    // question是列表
                                                    questions = (List<Map<String, Object>>) questionObj;
                                                } else if (questionObj instanceof Map) {
                                                    // question是单个对象
                                                    questions.add((Map<String, Object>) questionObj);
                                                }
                                                // 使用实际的题目数量，不补充默认题目
                                                log.info("试卷 {} 实际题目数量: {}", paper.get("paperName"), questions.size());
                                            } else {
                                                // 处理questions对象中的所有问题
                                                for (Map.Entry<String, Object> entry : questionsMap.entrySet()) {
                                                    Object value = entry.getValue();
                                                    if (value instanceof Map) {
                                                        questions.add((Map<String, Object>) value);
                                                    }
                                                }
                                                // 使用实际的题目数量，不补充默认题目
                                                log.info("试卷 {} 实际题目数量: {}", paper.get("paperName"), questions.size());
                                            }
                                        } else {
                                            // 无法识别的questions类型，生成默认题目
                                            log.warn("试卷 {} 题目格式错误，生成默认题目", paper.get("paperName"));
                                            questions = generateDefaultQuestions(questionCount, subjectName);
                                        }
                                    }
                                    
                                    // 更新questionCount为实际的题目数量
                                    paper.put("questionCount", questions.size());
                                    log.info("试卷 {} 已更新questionCount为: {}", paper.get("paperName"), questions.size());
                                    
                                    // 验证每道题的字段
                                    for (int i = 0; i < questions.size(); i++) {
                                        Map<String, Object> question = questions.get(i);
                                        if (!question.containsKey("id")) {
                                            question.put("id", "q-" + i);
                                        }
                                        if (!question.containsKey("type")) {
                                            question.put("type", "选择题");
                                        }
                                        if (!question.containsKey("content") || !question.containsKey("question")) {
                                            question.put("content", "第" + (i + 1) + "题");
                                            question.put("question", "第" + (i + 1) + "题");
                                        }
                                        if (!question.containsKey("options") && "选择题".equals(question.get("type"))) {
                                            question.put("options", Arrays.asList("A. 选项A", "B. 选项B", "C. 选项C", "D. 选项D"));
                                        }
                                        if (!question.containsKey("correctAnswer")) {
                                            question.put("correctAnswer", 0);
                                        }
                                        if (!question.containsKey("analysis")) {
                                            question.put("analysis", "本题考查" + subjectName + "的基础知识。");
                                        }
                                        if (!question.containsKey("score")) {
                                            question.put("score", (int) paper.get("totalScore") / questionCount);
                                        }
                                    }
                                    
                                    // 更新试卷的题目列表
                                    paper.put("questions", questions);
                                    
                                    // 添加创建时间
                                    paper.put("createTime", LocalDateTime.now());
                                }
                                
                                log.info("AI生成真题推荐成功，获取到 {} 份真题", papers.size());
                            } else {
                                // 如果不是列表，尝试提取部分数据
                                log.warn("AI返回的JSON对象中的examPapers不是列表，尝试提取部分数据");
                                papers = extractPartialExamData(aiResponse, subjectId, subjectName);
                            }
                        } else if (jsonObject.containsKey("papers") || jsonObject.containsKey("items") || jsonObject.containsKey("data")) {
                            // 兼容旧格式
                            Object papersObject = jsonObject.get("papers") != null ? jsonObject.get("papers") : 
                                              jsonObject.get("items") != null ? jsonObject.get("items") : 
                                              jsonObject.get("data");
                            
                            if (papersObject instanceof List) {
                                papers = (List<Map<String, Object>>) papersObject;
                                
                                // 验证并处理数据
                                for (Map<String, Object> paper : papers) {
                                    // 确保所有必要字段存在
                                    if (!paper.containsKey("id")) {
                                        paper.put("id", subjectId + "-ai-" + papers.indexOf(paper));
                                        // 使用固定的ID格式，避免时间戳导致的ID不匹配
                                    }
                                    if (!paper.containsKey("year")) {
                                        paper.put("year", "2024");
                                    }
                                    if (!paper.containsKey("paperName")) {
                                        paper.put("paperName", "AI推荐真题");
                                    }
                                    if (!paper.containsKey("subjectId")) {
                                        paper.put("subjectId", subjectId);
                                    }
                                    if (!paper.containsKey("subjectName")) {
                                        paper.put("subjectName", subjectName);
                                    }
                                    if (!paper.containsKey("duration")) {
                                        paper.put("duration", 120);
                                    }
                                    if (!paper.containsKey("totalScore")) {
                                        paper.put("totalScore", 100);
                                    }
                                    if (!paper.containsKey("questionCount")) {
                                        paper.put("questionCount", 25);
                                    }
                                    if (!paper.containsKey("difficulty")) {
                                        paper.put("difficulty", 3);
                                    }
                                    if (!paper.containsKey("recommendReason")) {
                                        paper.put("recommendReason", "AI推荐的优质真题");
                                    }
                                    if (!paper.containsKey("source")) {
                                        paper.put("source", "AI生成");
                                    }
                                    // 添加创建时间
                                    paper.put("createTime", LocalDateTime.now());
                                }
                                
                                log.info("AI生成真题推荐成功，获取到 {} 份真题", papers.size());
                            } else {
                                // 如果不是列表，尝试提取部分数据
                                log.warn("AI返回的JSON对象不包含试卷列表，尝试提取部分数据");
                                papers = extractPartialExamData(aiResponse, subjectId, subjectName);
                            }
                        } else {
                            // 如果对象中没有试卷列表，尝试提取部分数据
                            log.warn("AI返回的JSON对象不包含试卷列表，尝试提取部分数据");
                            papers = extractPartialExamData(aiResponse, subjectId, subjectName);
                        }
                    } catch (Exception e2) {
                        log.warn("解析JSON对象失败: {}", e2.getMessage());
                        
                        // 只有当检测到的是数组类型时才尝试解析为数组
                        if (token == JsonToken.START_ARRAY) {
                            try {
                                log.info("尝试解析JSON数组...");
                                papers = objectMapper.readValue(aiResponse, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                                
                                log.info("成功解析为JSON数组，包含 {} 份试卷", papers.size());
                                
                                // 验证并处理数据
                                for (Map<String, Object> paper : papers) {
                                    // 确保所有必要字段存在
                                    if (!paper.containsKey("id")) {
                                        paper.put("id", subjectId + "-ai-" + papers.indexOf(paper));
                                        // 使用固定的ID格式，避免时间戳导致的ID不匹配
                                    }
                                    if (!paper.containsKey("year")) {
                                        paper.put("year", "2024");
                                    }
                                    if (!paper.containsKey("paperName")) {
                                        paper.put("paperName", "AI推荐真题");
                                    }
                                    if (!paper.containsKey("subjectId")) {
                                        paper.put("subjectId", subjectId);
                                    }
                                    if (!paper.containsKey("subjectName")) {
                                        paper.put("subjectName", subjectName);
                                    }
                                    if (!paper.containsKey("duration")) {
                                        paper.put("duration", 120);
                                    }
                                    if (!paper.containsKey("totalScore")) {
                                        paper.put("totalScore", 100);
                                    }
                                    if (!paper.containsKey("questionCount")) {
                                        paper.put("questionCount", 25);
                                    }
                                    if (!paper.containsKey("difficulty")) {
                                        paper.put("difficulty", 3);
                                    }
                                    if (!paper.containsKey("recommendReason")) {
                                        paper.put("recommendReason", "AI推荐的优质真题");
                                    }
                                    if (!paper.containsKey("source")) {
                                        paper.put("source", "AI生成");
                                    }
                                    // 添加创建时间
                                    paper.put("createTime", LocalDateTime.now());
                                }

                                log.info("AI生成真题推荐成功，获取到 {} 份真题", papers.size());
                            } catch (Exception e3) {
                                // AI返回的不是有效的JSON，尝试提取部分数据
                                log.warn("解析JSON数组失败: {}", e3.getMessage());
                                log.warn("AI返回的响应不是有效的JSON，尝试提取部分数据");
                                log.debug("AI响应内容: {}", aiResponse);
                                log.debug("解析错误栈: ", e3);
                                papers = extractPartialExamData(aiResponse, subjectId, subjectName);
                            }
                        } else {
                            // 如果不是数组类型，尝试提取部分数据
                            log.warn("AI返回的响应不是有效的JSON对象，尝试提取部分数据");
                            log.debug("AI响应内容: {}", aiResponse);
                            papers = extractPartialExamData(aiResponse, subjectId, subjectName);
                        }
                    }
                }
            } catch (Exception e) {
                // 其他错误，尝试提取部分数据
                log.warn("处理AI响应时发生错误，尝试提取部分数据: {}", e.getMessage());
                log.debug("AI响应内容: {}", aiResponse);
                papers = extractPartialExamData(aiResponse, subjectId, subjectName);
            }

            // 将结果存入Redis缓存
            try {
                redisTemplate.opsForValue().set(cacheKey, papers, java.time.Duration.ofHours(24));
                log.info("真题推荐数据已缓存到Redis，缓存键: {}", cacheKey);
            } catch (Exception e) {
                log.warn("缓存真题推荐数据到Redis失败: {}", e.getMessage());
            }

            return papers;
        } catch (Exception e) {
            log.error("AI生成真题推荐失败: {}", e.getMessage(), e);
            // 失败时返回默认数据
            ExamSubject subject = examSubjectMapper.selectById(subjectId);
            String subjectName = subject != null ? subject.getName() : "未知科目";
            return getDefaultExamPapers(subjectId, subjectName);
        }
    }

    private List<Map<String, Object>> getDefaultExamPapers(String subjectId, String subjectName) {
        List<Map<String, Object>> papers = new ArrayList<>();
        papers.add(createPaper("2024", "AI推荐真题卷A", subjectId, subjectName, 120, 100));
        papers.add(createPaper("2024", "AI推荐真题卷B", subjectId, subjectName, 120, 100));
        papers.add(createPaper("2023", "AI推荐模拟卷", subjectId, subjectName, 90, 100));
        return papers;
    }

    private Map<String, Object> createPaper(String year, String paperName, String subjectId, String subjectName, int duration, int totalScore) {
        Map<String, Object> paper = new HashMap<>();
        paper.put("id", subjectId + "-" + year + "-" + System.currentTimeMillis());
        paper.put("year", year);
        paper.put("paperName", paperName);
        paper.put("subjectId", subjectId);
        paper.put("subjectName", subjectName);
        paper.put("duration", duration);
        paper.put("totalScore", totalScore);
        paper.put("questionCount", 20 + new Random().nextInt(10));
        paper.put("difficulty", new Random().nextInt(3) + 1); // 1-3 难度等级
        paper.put("createTime", LocalDateTime.now());
        return paper;
    }

    /**
     * 生成默认题目列表
     * @param count 题目数量
     * @param subjectName 科目名称
     * @return 题目列表
     */
    private List<Map<String, Object>> generateDefaultQuestions(int count, String subjectName) {
        List<Map<String, Object>> questions = new ArrayList<>();
        String[] questionTypes = {"选择题", "填空题", "解答题"};
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> question = new HashMap<>();
            String type = questionTypes[i % 3];
            
            question.put("id", "q-" + i);
            question.put("type", type);
            question.put("content", "第" + (i + 1) + "题：这是一道" + subjectName + "相关的" + type + "，请认真作答。");
            question.put("question", "第" + (i + 1) + "题：这是一道" + subjectName + "相关的" + type + "，请认真作答。");
            
            if ("选择题".equals(type)) {
                question.put("options", Arrays.asList(
                    "A. " + subjectName + "基础概念A",
                    "B. " + subjectName + "基础概念B",
                    "C. " + subjectName + "基础概念C",
                    "D. " + subjectName + "基础概念D"
                ));
                question.put("correctAnswer", i % 4);
            } else {
                question.put("correctAnswer", "正确答案");
            }
            
            question.put("analysis", "本题考查" + subjectName + "的" + type + "知识点。" + 
                ("选择题".equals(type) ? "正确答案是选项" + (char)('A' + (i % 4)) + "。" : "需要详细解答过程。"));
            question.put("score", 100 / count);
            
            questions.add(question);
        }
        
        return questions;
    }

    public List<Map<String, Object>> getExamHistory() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("用户 {} 查询考试历史", username);
            
            User user = userMapper.findByUsername(username);
            if (user == null) {
                log.error("用户 {} 不存在", username);
                throw new RuntimeException("用户不存在");
            }
            
            List<ExamHistory> histories = examHistoryMapper.selectByUserId(user.getId());
            List<Map<String, Object>> examHistory = histories.stream()
                .map(history -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", history.getId());
                    map.put("subject", history.getSubject());
                    map.put("score", history.getScore());
                    map.put("date", history.getExamDate());
                    map.put("duration", history.getDuration());
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("获取到 {} 条考试历史记录", examHistory.size());
            return examHistory;
        } catch (Exception e) {
            log.error("获取考试历史失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取考试历史失败，请稍后重试");
        }
    }

    public List<Map<String, Object>> getBooks() {
        try {
            log.info("获取书籍列表");
            List<Book> books = bookMapper.selectList(null);
            if (books == null) {
                log.info("书籍列表为空");
                return new ArrayList<>();
            }
            List<Map<String, Object>> bookList = books.stream()
                .map(book -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", book.getId());
                    map.put("name", book.getTitle());
                    map.put("author", book.getAuthor());
                    map.put("coverImage", book.getCoverImage());
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("获取到 {} 本书籍", bookList.size());
            return bookList;
        } catch (Exception e) {
            log.error("获取书籍列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取书籍列表失败，请稍后重试");
        }
    }

    public List<Map<String, Object>> getCourses() {
        try {
            log.info("获取课程列表");
            List<Map<String, Object>> courseList = new ArrayList<>();
            
            try {
                // 从数据库获取课程
                List<Course> courses = courseMapper.selectList(null);
                log.info("数据库课程数量: {} 个课程", courses != null ? courses.size() : 0);
                
                if (courses != null && !courses.isEmpty()) {
                    courseList = courses.stream()
                        .map(course -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", course.getId());
                            map.put("name", course.getName());
                            
                            // 使用AI生成推荐内容和视频链接
                            Map<String, Object> aiResult = generateRecommendationAndVideoLink();
                            
                            map.put("desc", aiResult.get("recommendation"));
                            map.put("up主", "系统推荐");
                            map.put("封面图片地址", "https://picsum.photos/320/180?random=1");
                            map.put("videoLink", aiResult.get("videoLink"));
                            return map;
                        })
                        .collect(Collectors.toList());
                    log.info("成功生成课程列表，共 {} 个课程", courseList.size());
                }
            } catch (Exception e) {
                log.error("获取课程失败: {}", e.getMessage(), e);
            }
            
            // 如果没有数据，返回默认数据
            if (courseList.isEmpty()) {
                Map<String, Object> defaultCourse = new HashMap<>();
                defaultCourse.put("id", 1);
                defaultCourse.put("name", "网课推荐");
                
                // 使用AI生成推荐内容和视频链接
                Map<String, Object> aiResult = generateRecommendationAndVideoLink();
                
                defaultCourse.put("desc", aiResult.get("recommendation"));
                defaultCourse.put("up主", "系统推荐");
                defaultCourse.put("封面图片地址", "https://picsum.photos/320/180?random=2");
                defaultCourse.put("videoLink", aiResult.get("videoLink"));
                courseList.add(defaultCourse);
            }
            
            log.info("获取到 {} 个课程", courseList.size());
            return courseList;
        } catch (Exception e) {
            log.error("获取课程列表失败: {}", e.getMessage(), e);
            // 返回默认数据
            List<Map<String, Object>> defaultCourseList = new ArrayList<>();
            Map<String, Object> defaultCourse = new HashMap<>();
            defaultCourse.put("id", 1);
            defaultCourse.put("name", "网课推荐");
            
            // 使用AI生成推荐内容和视频链接
            Map<String, Object> aiResult = generateRecommendationAndVideoLink();
            
            defaultCourse.put("desc", aiResult.get("recommendation"));
            defaultCourse.put("up主", "系统推荐");
            defaultCourse.put("封面图片地址", "https://picsum.photos/320/180?random=3");
            defaultCourse.put("videoLink", aiResult.get("videoLink"));
            defaultCourseList.add(defaultCourse);
            return defaultCourseList;
        }
    }
    
    /**
     * 获取最近的问答记录作为上下文
     * @return 最近问答记录的文本
     */
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
    
    /**
     * 预处理AI响应，提取JSON数据
     * @param aiResponse AI返回的原始响应
     * @return 预处理后的JSON字符串
     */
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
    
    /**
     * 检查JSON是否完整
     */
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
    
    /**
     * 清理和修复JSON数据
     * @param json 原始JSON字符串
     * @return 清理修复后的JSON字符串
     */
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
     * 从截断的JSON中提取部分试卷数据
     * @param aiResponse AI返回的原始响应
     * @param subjectId 科目ID
     * @param subjectName 科目名称
     * @return 提取的试卷列表
     */
    private List<Map<String, Object>> extractPartialExamData(String aiResponse, String subjectId, String subjectName) {
        List<Map<String, Object>> papers = new ArrayList<>();
        
        try {
            // 尝试从截断的JSON中提取试卷信息
            // 查找试卷相关的关键字段
            String[] paperKeywords = {"paperName", "year", "duration", "totalScore", "questionCount", "difficulty", "recommendReason", "source"};
            
            // 简单的提取逻辑，基于字符串匹配
            // 查找第一个试卷的开始位置
            int paperStart = aiResponse.indexOf("{");
            if (paperStart != -1) {
                // 尝试提取至少一个试卷
                Map<String, Object> paper = new HashMap<>();
                paper.put("id", subjectId + "-ai-" + System.currentTimeMillis() + "-partial");
                paper.put("subjectId", subjectId);
                paper.put("subjectName", subjectName);
                
                // 提取试卷名称
                int paperNameStart = aiResponse.indexOf("paperName", paperStart);
                if (paperNameStart != -1) {
                    int colonIndex = aiResponse.indexOf(":", paperNameStart);
                    int quoteStart = aiResponse.indexOf("\"", colonIndex + 1);
                    int quoteEnd = aiResponse.indexOf("\"", quoteStart + 1);
                    if (quoteStart != -1 && quoteEnd != -1) {
                        String paperName = aiResponse.substring(quoteStart + 1, quoteEnd);
                        paper.put("paperName", paperName);
                    }
                }
                
                // 提取年份
                int yearStart = aiResponse.indexOf("year", paperStart);
                if (yearStart != -1) {
                    int colonIndex = aiResponse.indexOf(":", yearStart);
                    int valueStart = colonIndex + 1;
                    // 跳过空白字符
                    while (valueStart < aiResponse.length() && Character.isWhitespace(aiResponse.charAt(valueStart))) {
                        valueStart++;
                    }
                    // 提取年份值
                    StringBuilder yearValue = new StringBuilder();
                    while (valueStart < aiResponse.length() && (Character.isDigit(aiResponse.charAt(valueStart)) || aiResponse.charAt(valueStart) == '"')) {
                        yearValue.append(aiResponse.charAt(valueStart));
                        valueStart++;
                    }
                    String yearStr = yearValue.toString().replace("\"", "").trim();
                    if (!yearStr.isEmpty()) {
                        paper.put("year", yearStr);
                    }
                }
                
                // 设置默认值
                if (!paper.containsKey("paperName")) {
                    paper.put("paperName", "AI推荐真题");
                }
                if (!paper.containsKey("year")) {
                    paper.put("year", "2024");
                }
                paper.put("duration", 120);
                paper.put("totalScore", 100);
                paper.put("questionCount", 25);
                paper.put("difficulty", 3);
                paper.put("recommendReason", "AI推荐的优质真题");
                paper.put("source", "AI生成");
                
                // 生成默认题目
                List<Map<String, Object>> questions = generateDefaultQuestions(25, subjectName);
                paper.put("questions", questions);
                paper.put("createTime", LocalDateTime.now());
                
                papers.add(paper);
                log.info("成功从截断的JSON中提取部分试卷数据");
            } else {
                // 如果无法提取，使用默认数据
                log.warn("无法从截断的JSON中提取数据，使用默认数据");
                papers = getDefaultExamPapers(subjectId, subjectName);
            }
        } catch (Exception e) {
            log.error("提取部分数据失败: {}", e.getMessage());
            // 失败时使用默认数据
            papers = getDefaultExamPapers(subjectId, subjectName);
        }
        
        return papers;
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
     * 生成推荐内容和视频链接
     * @return 包含推荐内容和视频链接的Map
     */
    private Map<String, Object> generateRecommendationAndVideoLink() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取最近的问答记录作为上下文
            String recentQaContext = getRecentQaContext();
            boolean hasQaHistory = !recentQaContext.isEmpty();
            
            if (hasQaHistory) {
                // 有问答记录，使用上下文生成个性化推荐
                String jsonPrompt = "基于用户最近的学习情况，生成一个包含推荐内容和视频链接的JSON对象，格式如下：\n" +
                        "{\n" +
                        "  \"recommendation\": \"个性化的学习资源推荐文本\",\n" +
                        "  \"videoLink\": \"中国大陆可访问的学习视频完整URL链接\"\n" +
                        "}\n" +
                        "推荐的视频资源请使用中国大陆可访问的平台，如B站（bilibili）、慕课网、中国大学MOOC等。\n" +
                        "只返回JSON格式，不要包含其他解释性文本。";
                
                String aiResponse = aiService.generateResponseWithContext(recentQaContext, jsonPrompt);
                aiResponse = preprocessAiResponse(aiResponse);
                
                // 解析AI返回的JSON
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    Map<String, Object> aiResult = objectMapper.readValue(aiResponse, Map.class);
                    
                    // 验证链接格式
                    String videoLink = (String) aiResult.get("videoLink");
                    if (videoLink != null && (!videoLink.startsWith("http://") && !videoLink.startsWith("https://"))) {
                        videoLink = "https://www.bilibili.com/video/BV1Gx411w7La";
                        aiResult.put("videoLink", videoLink);
                    }
                    
                    // 提取并验证链接
                    if (videoLink != null) {
                        videoLink = extractAndValidateUrl(videoLink);
                        aiResult.put("videoLink", videoLink);
                    }
                    
                    result.putAll(aiResult);
                    
                    log.info("AI生成推荐内容成功，视频链接: {}", aiResult.get("videoLink"));
                } catch (Exception e) {
                    // AI返回的不是有效的JSON，使用默认推荐
                    log.warn("AI返回的响应不是有效的JSON，使用默认推荐: {}", e.getMessage());
                    log.debug("AI响应内容: {}", aiResponse);
                    result.put("recommendation", "为您推荐优质学习资源，包括数学、英语、物理等多个学科的精品课程。建议根据自身学习进度选择适合的课程，注重基础概念的理解，结合实际练习巩固知识点。同时，合理安排学习时间，保持学习的连续性和专注度。");
                    result.put("videoLink", "https://www.bilibili.com/video/BV1Gx411w7La");
                }
            } else {
                // 无问答记录，返回通用大学课程视频
                List<Map<String, String>> commonCourses = getCommonUniversityCourses();
                // 随机选择一个课程
                Map<String, String> selectedCourse = commonCourses.get(new Random().nextInt(commonCourses.size()));
                
                result.put("recommendation", selectedCourse.get("description"));
                result.put("videoLink", selectedCourse.get("url"));
                
                log.info("返回通用大学课程视频，链接: {}", selectedCourse.get("url"));
            }
        } catch (Exception e) {
            log.error("AI生成推荐内容失败: {}", e.getMessage());
            // 失败时返回默认国内平台链接
            result.put("recommendation", "为您推荐优质学习资源，包括数学、英语、物理等多个学科的精品课程。建议根据自身学习进度选择适合的课程，注重基础概念的理解，结合实际练习巩固知识点。同时，合理安排学习时间，保持学习的连续性和专注度。");
            result.put("videoLink", "https://www.bilibili.com/video/BV1Gx411w7La");
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

    /**
     * 提取并验证URL链接
     * @param text 包含链接的文本
     * @return 验证后的URL
     */
    private String extractAndValidateUrl(String text) {
        // 如果文本已经是有效的URL格式，直接返回
        if (text != null && (text.startsWith("http://") || text.startsWith("https://"))) {
            // 验证是否为国内可访问平台，如果是则直接返回
            if (isDomesticPlatform(text)) {
                return text;
            }
            // 如果不是国内平台，但仍然有效，也返回（可能AI推荐了其他有效平台）
            return text;
        }
        
        // 简单的URL提取逻辑
        // 更全面的URL正则表达式
        String urlPattern = "https?://(?:[\\w\\-]+\\.)+[\\w\\-]+(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=.]*)?";
        Pattern pattern = java.util.regex.Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String extractedUrl = matcher.group();
            return extractedUrl;
        }
        // 如果无法提取URL，返回原始文本（让前端处理）
        return text;
    }
    
    /**
     * 检查是否为国内可访问平台
     * @param url 视频链接
     * @return 是否为国内平台
     */
    private boolean isDomesticPlatform(String url) {
        String[] domesticPlatforms = {
            "bilibili.com",
            "icourse163.org",
            "mooc.cn",
            "youku.com",
            "tencent.com",
            "iqiyi.com"
        };
        
        for (String platform : domesticPlatforms) {
            if (url.contains(platform)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取通用大学课程视频列表
     * @return 课程列表
     */
    private List<Map<String, String>> getCommonUniversityCourses() {
        List<Map<String, String>> courses = new ArrayList<>();
        
        // 添加多个通用大学课程
        Map<String, String> course1 = new HashMap<>();
        course1.put("title", "高等数学（一）");
        course1.put("platform", "B站");
        course1.put("url", "https://www.bilibili.com/video/BV1dx411S7WX");
        course1.put("description", "高等数学是大学理工科的基础课程，本课程涵盖微积分、线性代数等核心内容，适合大学新生学习。");
        courses.add(course1);
        
        Map<String, String> course2 = new HashMap<>();
        course2.put("title", "大学物理");
        course2.put("platform", "B站");
        course2.put("url", "https://www.bilibili.com/video/BV1Vf4y1e7pC");
        course2.put("description", "大学物理课程，涵盖力学、电磁学、光学等内容，帮助学生建立完整的物理知识体系。");
        courses.add(course2);
        
        Map<String, String> course3 = new HashMap<>();
        course3.put("title", "大学英语");
        course3.put("platform", "中国大学MOOC");
        course3.put("url", "https://www.icourse163.org/course/TONGJI-1462506163");
        course3.put("description", "大学英语课程，提高学生的英语听说读写能力，为学术交流和未来工作做准备。");
        courses.add(course3);
        
        Map<String, String> course4 = new HashMap<>();
        course4.put("title", "数据结构与算法");
        course4.put("platform", "B站");
        course4.put("url", "https://www.bilibili.com/video/BV1Jv411A7Ty");
        course4.put("description", "数据结构与算法是计算机专业的核心课程，本课程介绍常见数据结构和经典算法。");
        courses.add(course4);
        
        Map<String, String> course5 = new HashMap<>();
        course5.put("title", "线性代数");
        course5.put("platform", "B站");
        course5.put("url", "https://www.bilibili.com/video/BV1aW411Q7x1");
        course5.put("description", "线性代数课程，涵盖矩阵、行列式、特征值等内容，是数学和工程学科的基础。");
        courses.add(course5);
        
        return courses;
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
    
    // 输入sanitization方法，防止XSS攻击
    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;");
    }

    private void updateSubjectPreference(Long userId, String subjectId, Integer result) {
        try {
            UserSubjectPreference preference = userSubjectPreferenceMapper.selectByUserIdAndSubjectId(userId, subjectId);
            if (preference == null) {
                // 创建新的偏好记录
                preference = new UserSubjectPreference();
                preference.setUserId(userId);
                preference.setSubjectId(subjectId);
                preference.setPreferenceScore(result == 1 ? 10 : 5);
                preference.setAnswerCount(1);
                preference.setCorrectCount(result);
                userSubjectPreferenceMapper.insert(preference);
            } else {
                // 更新现有记录
                preference.setAnswerCount(preference.getAnswerCount() + 1);
                preference.setCorrectCount(preference.getCorrectCount() + result);
                preference.setPreferenceScore(preference.getPreferenceScore() + (result == 1 ? 5 : -2));
                userSubjectPreferenceMapper.updateById(preference);
            }
        } catch (Exception e) {
            log.error("更新学科偏好失败: {}", e.getMessage(), e);
        }
    }

    private void updateWeakKnowledge(Long userId, String knowledgeId, String subjectId, Integer result) {
        try {
            // 这里简化处理，实际应该根据知识点名称等信息进行更复杂的逻辑
            UserWeakKnowledge weakKnowledge = new UserWeakKnowledge();
            weakKnowledge.setUserId(userId);
            weakKnowledge.setKnowledgeId(knowledgeId);
            weakKnowledge.setKnowledgeName("知识点" + knowledgeId);
            weakKnowledge.setSubjectId(subjectId);
            weakKnowledge.setMasteryLevel(result == 1 ? 80 : 30);
            weakKnowledge.setErrorCount(result == 0 ? 1 : 0);
            weakKnowledge.setLastErrorTime(result == 0 ? LocalDateTime.now() : null);
            weakKnowledge.setCreatedAt(LocalDateTime.now());
            weakKnowledge.setUpdatedAt(LocalDateTime.now());
            userWeakKnowledgeMapper.insert(weakKnowledge);
        } catch (Exception e) {
            log.error("更新薄弱知识点失败: {}", e.getMessage(), e);
        }
    }

    private void updateUserProfileStats(Long userId) {
        try {
            UserProfile profile = userProfileMapper.selectByUserId(userId);
            if (profile != null) {
                profile.setStudyProgress(profile.getStudyProgress() + 1);
                profile.setLastStudyTime(LocalDateTime.now());
                userProfileMapper.updateById(profile);
            }
        } catch (Exception e) {
            log.error("更新用户画像统计失败: {}", e.getMessage(), e);
        }
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
            
            // 构建AI提示词
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
                    "          \"video_url\": \"UP主主页链接\",\n" +
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
                    "  ],\n" +
                    "  \"note\": \"cover_url字段暂为空，因搜索结果中未提供具体的课程封面图片链接。部分series的url字段为空，因该系列包含多个视频，建议访问UP主主页查看完整合集。\"\n" +
                    "}\n" +
                    "要求：\n" +
                    "1. 推荐的课程必须来自中国大陆可访问的平台，如智慧树、中国大学MOOC、国家高等教育智慧教育平台、B站等\n" +
                    "2. 每个类别至少包含2个课程\n" +
                    "3. 选择建议至少包含4个不同场景\n" +
                    "4. 直接返回JSON格式，不要包含其他解释性文本\n" +
                    "5. 基于用户最近的学习情况生成个性化推荐\n" +
                    "\n" +
                    "用户最近的学习情况：\n" + recentQaContext;
            
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
            
            // 验证并处理视频链接（仅做基本验证，保留AI推荐的原始链接）
            if (result.containsKey("categories")) {
                List<Map<String, Object>> categories = (List<Map<String, Object>>) result.get("categories");
                for (Map<String, Object> category : categories) {
                    if (category.containsKey("courses")) {
                        List<Map<String, Object>> courses = (List<Map<String, Object>>) category.get("courses");
                        for (Map<String, Object> course : courses) {
                            // 验证官方平台课程的视频链接
                            if (course.containsKey("video_url")) {
                                String videoUrl = (String) course.get("video_url");
                                if (videoUrl != null && !videoUrl.isEmpty()) {
                                    // 仅清理URL中的多余字符，保留AI推荐的原始链接
                                    String cleanedUrl = cleanUrl(videoUrl);
                                    course.put("video_url", cleanedUrl);
                                    log.debug("课程 {} 的视频链接: {}", course.get("name"), cleanedUrl);
                                }
                            }
                            // 验证B站课程的系列链接
                            if (course.containsKey("series")) {
                                List<Map<String, Object>> series = (List<Map<String, Object>>) course.get("series");
                                for (Map<String, Object> seriesItem : series) {
                                    if (seriesItem.containsKey("url")) {
                                        String seriesUrl = (String) seriesItem.get("url");
                                        if (seriesUrl != null && !seriesUrl.isEmpty()) {
                                            // 仅清理URL中的多余字符，保留AI推荐的原始链接
                                            String cleanedUrl = cleanUrl(seriesUrl);
                                            seriesItem.put("url", cleanedUrl);
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

    private Map<String, Object> convertExamQaToResponse(ExamQaHistory history) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", history.getId());
        response.put("question", history.getQuestion());
        response.put("answer", history.getAnswer());
        response.put("subject", history.getSubject());
        response.put("createdAt", history.getCreatedAt());
        return response;
    }

    @Override
    public List<Map<String, Object>> getExamCourses(String subjectId) {
        try {
            log.info("获取指定科目的课程，科目ID: {}", subjectId);
            List<Map<String, Object>> courseList = new ArrayList<>();
            
            try {
                // 从数据库获取课程
                List<Course> courses = courseMapper.selectList(null);
                log.info("数据库课程数量: {} 个课程", courses != null ? courses.size() : 0);
                
                if (courses != null && !courses.isEmpty()) {
                    courseList = courses.stream()
                        .map(course -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", course.getId());
                            map.put("name", course.getName());
                            
                            // 使用AI生成推荐内容和视频链接
                            Map<String, Object> aiResult = generateRecommendationAndVideoLink();
                            
                            map.put("desc", aiResult.get("recommendation"));
                            map.put("up主", "系统推荐");
                            map.put("封面图片地址", "https://picsum.photos/320/180?random=1");
                            map.put("videoLink", aiResult.get("videoLink"));
                            return map;
                        })
                        .collect(Collectors.toList());
                    log.info("成功生成课程列表，共 {} 个课程", courseList.size());
                }
            } catch (Exception e) {
                log.error("获取课程失败: {}", e.getMessage(), e);
            }
            
            // 如果没有数据，返回默认数据
            if (courseList.isEmpty()) {
                // 根据科目返回不同的默认课程
                switch (subjectId) {
                    case "builder" -> {
                        courseList.add(createBuilderCourse("建筑力学基础",
                                "建筑力学是建筑工程的基础课程，涵盖静力学、材料力学等内容",
                                "https://www.bilibili.com/video/BV1rnPvz8Eya/")); // 同济大学朱慈勉 结构力学完整课程
                        courseList.add(createBuilderCourse("建筑材料与构造",
                                "建筑材料与构造是建筑设计的基础，学习各种建筑材料的特性和应用",
                                "https://www.bilibili.com/video/BV1JW411u7sE/")); // 清华大学建筑材料精品课
                        courseList.add(createBuilderCourse("建筑设计原理",
                                "建筑设计原理是建筑专业的核心课程，学习建筑设计的基本理论和方法",
                                "https://www.bilibili.com/video/BV1hJ411s7XJ/")); // 东南大学建筑设计原理权威课程
                    }
                    case "math" -> {
                        courseList.add(createBuilderCourse("高等数学",
                                "高等数学是理工科的基础课程，涵盖微积分、线性代数等内容",
                                "https://www.bilibili.com/video/BV1Eb411u7R3/")); // 宋浩老师高数2.0版 强化速通课
                        courseList.add(createBuilderCourse("线性代数",
                                "线性代数是数学的重要分支，在工程和计算机科学中有广泛应用",
                                "https://www.bilibili.com/video/BV1zx411g7gq/")); // MIT 18.06 线性代数 Gilbert Strang教授经典课程(中英双字)
                    }
                    case "english" -> {
                        courseList.add(createBuilderCourse("大学英语",
                                "大学英语课程，提高学生的英语听说读写能力",
                                "https://www.icourse163.org/course/TONGJI-1462506163")); // 同济大学大学英语官方慕课
                        courseList.add(createBuilderCourse("英语听力",
                                "英语听力训练，提高听力理解能力",
                                "https://www.bilibili.com/video/BV1u6cZzNE18/")); // BBC 6 Minute English 完整合集(2008-2025)
                    }
                    case "physics" -> {
                        courseList.add(createBuilderCourse("大学物理",
                                "大学物理课程，涵盖力学、电磁学、光学等内容",
                                "https://www.bilibili.com/video/BV1Vf4y1e7pC/")); // 清华大学大学物理 安宇等教授主讲
                        courseList.add(createBuilderCourse("物理实验",
                                "物理实验课程，培养学生的实验技能和科学素养",
                                "https://open.163.com/newview/movie/theater?pid=EHJP1G91K")); // 北京大学物理学院普通物理实验教学录像合集(77集)
                    }
                    case "cs" -> {
                        courseList.add(createBuilderCourse("数据结构与算法",
                                "数据结构与算法是计算机专业的核心课程",
                                "https://www.bilibili.com/video/BV1JW411i731/")); // 浙江大学 陈越、何钦铭 数据结构经典课程
                        courseList.add(createBuilderCourse("计算机组成原理",
                                "计算机组成原理是计算机专业的基础课程",
                                "https://www.bilibili.com/video/BV1t4411e7LH/")); // 哈尔滨工业大学 刘宏伟 计算机组成原理完整课程(135讲)
                    }
                    default ->
                            courseList.add(createBuilderCourse("通用课程",
                                    "适合所有学科的通用课程",
                                    "https://www.bilibili.com/video/BV1Gx411w7La/")); // 高效学习技巧 费曼学习法+思维导图
                }
            }
            
            log.info("获取到 {} 个课程", courseList.size());
            return courseList;
        } catch (Exception e) {
            log.error("获取科目课程失败: {}", e.getMessage(), e);
            // 返回默认数据
            List<Map<String, Object>> defaultCourseList = new ArrayList<>();
            defaultCourseList.add(createBuilderCourse("默认课程", "系统推荐的默认课程", "https://www.bilibili.com/video/BV1Gx411w7La"));
            return defaultCourseList;
        }
    }
    
    private Map<String, Object> createBuilderCourse(String name, String desc, String videoLink) {
        Map<String, Object> course = new HashMap<>();
        course.put("id", System.currentTimeMillis());
        course.put("name", name);
        course.put("desc", desc);
        course.put("up主", "系统推荐");
        course.put("封面图片地址", "https://picsum.photos/320/180?random=" + System.currentTimeMillis());
        course.put("videoLink", videoLink);
        return course;
    }

    @Override
    public List<Map<String, Object>> getExamPaperQuestions(String paperId) {
        try {
            log.info("获取试卷题目，试卷ID: {}", paperId);
            List<Map<String, Object>> questions = new ArrayList<>();
            
            // 尝试从Redis缓存中获取AI生成的试卷数据
            try {
                // 从试卷ID中提取科目ID
                String subjectId = extractSubjectIdFromPaperId(paperId);
                if (subjectId != null) {
                    // 使用与generateExamPapersByAI方法相同的缓存键格式
                    String cacheKey = "exam:papers:ai:" + subjectId;
                    List<Map<String, Object>> papers = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
                    
                    if (papers != null && !papers.isEmpty()) {
                        for (Map<String, Object> paper : papers) {
                            String cachedPaperId = (String) paper.get("id");
                            if (paperId.equals(cachedPaperId)) {
                                // 找到匹配的试卷，提取题目
                                Object questionsObj = paper.get("questions");
                                if (questionsObj instanceof List) {
                                    questions = (List<Map<String, Object>>) questionsObj;
                                    log.info("从Redis缓存中获取到试卷题目，共 {} 道", questions.size());
                                    return questions;
                                }
                            }
                        }
                    }
                }
                
                // 如果从特定科目缓存中找不到，尝试遍历所有可能的缓存
                String[] possibleSubjects = {"builder", "math", "english", "politics"};
                for (String subject : possibleSubjects) {
                    String cacheKey = "exam:papers:ai:" + subject;
                    List<Map<String, Object>> papers = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
                    
                    if (papers != null && !papers.isEmpty()) {
                        for (Map<String, Object> paper : papers) {
                            String cachedPaperId = (String) paper.get("id");
                            if (paperId.equals(cachedPaperId)) {
                                // 找到匹配的试卷，提取题目
                                Object questionsObj = paper.get("questions");
                                if (questionsObj instanceof List) {
                                    questions = (List<Map<String, Object>>) questionsObj;
                                    log.info("从Redis缓存中获取到试卷题目，共 {} 道", questions.size());
                                    return questions;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("从Redis缓存获取题目失败: {}", e.getMessage());
            }
            
            // 如果从缓存中找不到题目，根据试卷ID生成对应的练习题目
            if (paperId != null && paperId.contains("builder")) {
                // 建筑类试卷题目
                questions.add(createQuestion("1", "选择题", "根据《建筑法》，建筑工程开工前，建设单位应当按照国家有关规定向工程所在地县级以上人民政府建设行政主管部门申请领取施工许可证。", 
                    Arrays.asList("A. 施工许可证", "B. 规划许可证", "C. 用地许可证", "D. 环评许可证"), 0, "本题考查建筑法基础知识，施工许可是开工的必要条件。", "easy", 5));
                questions.add(createQuestion("2", "选择题", "在混凝土结构中，钢筋的保护层厚度主要作用是（ ）。", 
                    Arrays.asList("A. 美观", "B. 防火", "C. 保护钢筋免受腐蚀", "D. 增加强度"), 2, "钢筋保护层的主要作用是防止钢筋锈蚀，保证结构耐久性。", "medium", 5));
                questions.add(createQuestion("3", "选择题", "建筑工程项目管理的核心任务是（ ）。", 
                    Arrays.asList("A. 成本控制", "B. 目标控制", "C. 质量控制", "D. 进度控制"), 1, "项目管理的核心任务是目标控制，包括成本、质量、进度等多方面目标。", "medium", 5));
                questions.add(createQuestion("4", "填空题", "混凝土的强度等级是按______强度标准值划分的。", 
                    null, 0, "混凝土强度等级按立方体抗压强度标准值划分，如C30表示30MPa。", "medium", 5));
                questions.add(createQuestion("5", "选择题", "根据《招标投标法》，依法必须进行招标的项目，自招标文件开始发出之日起至投标人提交投标文件截止之日止，最短不得少于（ ）日。", 
                    Arrays.asList("A. 10", "B. 15", "C. 20", "D. 30"), 2, "依法必须招标的项目，招标文件发售至投标截止不得少于20日。", "easy", 5));
            } else if (paperId != null && paperId.contains("math")) {
                // 数学类试卷题目
                questions.add(createQuestion("1", "选择题", "求函数 f(x) = x² - 4x + 3 的极小值。", 
                    Arrays.asList("A. -1", "B. 0", "C. 1", "D. 3"), 0, "通过求导或配方法可得，f(x)在x=2处取得极小值-1。", "medium", 5));
                questions.add(createQuestion("2", "选择题", "计算极限 lim(x→0) (sin x)/x 的值。", 
                    Arrays.asList("A. 0", "B. 1", "C. ∞", "D. 不存在"), 1, "这是重要极限之一，lim(x→0) (sin x)/x = 1。", "easy", 5));
                questions.add(createQuestion("3", "填空题", "矩阵 A = [[1,2],[3,4]] 的行列式值为______。", 
                    null, 0, "行列式 |A| = 1×4 - 2×3 = 4 - 6 = -2。", "medium", 5));
            } else {
                // 默认通用题目
                questions.add(createQuestion("1", "选择题", "这是一道示例选择题，请选择正确答案。", 
                    Arrays.asList("A. 选项A", "B. 选项B", "C. 选项C", "D. 选项D"), 0, "这是示例解析，实际应用中应根据具体题目提供详细解析。", "medium", 5));
                questions.add(createQuestion("2", "填空题", "请填写正确答案______。", 
                    null, 0, "填空题解析，说明答案的推导过程。", "medium", 5));
                questions.add(createQuestion("3", "解答题", "请详细解答以下问题，并说明解题思路。", 
                    null, 0, "解答题需要详细的解题步骤和思路说明。", "hard", 10));
            }
            
            log.info("成功生成 {} 道试卷题目", questions.size());
            return questions;
        } catch (Exception e) {
            log.error("获取试卷题目失败: {}", e.getMessage(), e);
            // 返回默认题目
            List<Map<String, Object>> defaultQuestions = new ArrayList<>();
            defaultQuestions.add(createQuestion("1", "选择题", "默认示例题目", 
                Arrays.asList("A. 选项A", "B. 选项B", "C. 选项C", "D. 选项D"), 0, "默认解析", "medium", 5));
            return defaultQuestions;
        }
    }
    
    private Map<String, Object> createQuestion(String id, String type, String content, 
            List<String> options, int correctAnswer, String analysis, String difficulty, int score) {
        Map<String, Object> question = new HashMap<>();
        question.put("id", id);
        question.put("type", type);
        question.put("content", content);
        question.put("question", content);
        question.put("title", content);
        if (options != null) {
            question.put("options", options);
        }
        question.put("correctAnswer", correctAnswer);
        question.put("analysis", analysis);
        question.put("explanation", analysis);
        question.put("difficulty", difficulty);
        question.put("score", score);
        question.put("knowledgePoint", "相关知识");
        return question;
    }
    
    /**
     * 从试卷ID中提取科目ID
     * @param paperId 试卷ID
     * @return 科目ID
     */
    private String extractSubjectIdFromPaperId(String paperId) {
        if (paperId == null) {
            return null;
        }
        
        // 尝试从试卷ID中提取科目ID
        // 试卷ID格式可能是：builder_2024_01 或 builder-ai-1234567890-0
        if (paperId.contains("_")) {
            return paperId.split("_")[0];
        } else if (paperId.contains("-")) {
            String[] parts = paperId.split("-");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        
        // 如果无法提取，返回null
        return null;
    }

    @Override
    public List<Map<String, Object>> getChapters(Long bookId) {
        try {
            log.info("获取书籍章节，书籍ID: {}", bookId);
            List<BookChapter> chapters = bookChapterMapper.selectByBookId(bookId);
            if (chapters == null) {
                log.info("章节列表为空");
                return new ArrayList<>();
            }
            
            // 将章节转换为树形结构
            List<Map<String, Object>> chapterTree = buildChapterTree(chapters);
            
            log.info("获取到 {} 个章节", chapterTree.size());
            return chapterTree;
        } catch (Exception e) {
            log.error("获取章节列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取章节列表失败，请稍后重试");
        }
    }
    
    /**
     * 将扁平的章节列表构建为树形结构
     * @param chapters 扁平的章节列表
     * @return 树形结构的章节列表
     */
    private List<Map<String, Object>> buildChapterTree(List<BookChapter> chapters) {
        // 创建一个Map来存储所有章节，方便查找
        Map<Long, Map<String, Object>> chapterMap = new HashMap<>();
        List<Map<String, Object>> rootChapters = new ArrayList<>();
        
        // 第一次遍历：创建所有章节节点
        for (BookChapter chapter : chapters) {
            Map<String, Object> chapterNode = new HashMap<>();
            chapterNode.put("id", chapter.getId());
            chapterNode.put("bookId", chapter.getBookId());
            chapterNode.put("title", chapter.getTitle());
            chapterNode.put("content", chapter.getContent());
            chapterNode.put("sortOrder", chapter.getSortOrder());
            chapterNode.put("parentId", chapter.getParentId());
            chapterNode.put("status", chapter.getStatus());
            chapterNode.put("children", new ArrayList<Map<String, Object>>());
            chapterMap.put(chapter.getId(), chapterNode);
        }
        
        // 第二次遍历：构建父子关系
        for (BookChapter chapter : chapters) {
            Map<String, Object> chapterNode = chapterMap.get(chapter.getId());
            Long parentId = chapter.getParentId();
            
            if (parentId == null || parentId == 0) {
                // 根章节
                rootChapters.add(chapterNode);
            } else {
                // 子章节
                Map<String, Object> parentChapter = chapterMap.get(parentId);
                if (parentChapter != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parentChapter.get("children");
                    children.add(chapterNode);
                } else {
                    // 父章节不存在，作为根章节处理
                    rootChapters.add(chapterNode);
                }
            }
        }
        
        // 按sortOrder排序根章节
        rootChapters.sort((a, b) -> {
            Integer sortOrderA = (Integer) a.get("sortOrder");
            Integer sortOrderB = (Integer) b.get("sortOrder");
            if (sortOrderA == null) sortOrderA = 0;
            if (sortOrderB == null) sortOrderB = 0;
            return sortOrderA.compareTo(sortOrderB);
        });
        
        // 递归排序子章节
        for (Map<String, Object> chapter : rootChapters) {
            sortChildren(chapter);
        }
        
        return rootChapters;
    }
    
    /**
     * 递归排序子章节
     * @param chapter 章节节点
     */
    @SuppressWarnings("unchecked")
    private void sortChildren(Map<String, Object> chapter) {
        List<Map<String, Object>> children = (List<Map<String, Object>>) chapter.get("children");
        if (children != null && !children.isEmpty()) {
            children.sort((a, b) -> {
                Integer sortOrderA = (Integer) a.get("sortOrder");
                Integer sortOrderB = (Integer) b.get("sortOrder");
                if (sortOrderA == null) sortOrderA = 0;
                if (sortOrderB == null) sortOrderB = 0;
                return sortOrderA.compareTo(sortOrderB);
            });
            // 递归排序子章节的子章节
            for (Map<String, Object> child : children) {
                sortChildren(child);
            }
        }
    }
    
    /**
     * 解析试卷JSON数据
     * @param jsonData JSON字符串
     * @return 试卷列表
     */
    public List<ExamPaper> parseExamPapers(String jsonData) {
        try {
            log.debug("开始解析试卷JSON数据");
            
            // 1. 清理JSON字符串
            String cleanedJson = cleanJsonString(jsonData);
            
            // 2. 解析为ExamPaperResponse对象
            ExamPaperResponse response = mapper.readValue(cleanedJson, ExamPaperResponse.class);
            
            // 3. 获取试卷列表
            List<ExamPaper> examPapers = response.getExamPapers();
            
            log.debug("解析成功，获取到 {} 份试卷", examPapers.size());
            return examPapers;
            
        } catch (Exception e) {
            log.error("解析错误栈: ", e);
            
            // 记录原始JSON数据（截断）
            String truncatedJson = jsonData.length() > 500 ? 
                jsonData.substring(0, 500) + "..." : jsonData;
            log.error("原始JSON数据（前500字符）: {}", truncatedJson);
            
            throw new RuntimeException("试卷数据解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 通用解析方法 - 支持多种格式
     * @param jsonData JSON字符串
     * @return 试卷列表
     */
    public List<ExamPaper> parseExamPapersFlexibly(String jsonData) {
        try {
            String cleanedJson = cleanJsonString(jsonData);
            
            // 先尝试解析为JsonNode查看结构
            JsonNode rootNode = mapper.readTree(cleanedJson);
            
            log.debug("JSON根节点类型: {}", rootNode.getNodeType());
            
            if (rootNode.isArray()) {
                // 如果是数组格式，直接解析
                return mapper.convertValue(rootNode, new TypeReference<List<ExamPaper>>() {});
                
            } else if (rootNode.isObject()) {
                // 如果是对象格式，检查常见字段
                String[] possibleArrayFields = { 
                    "examPapers", "papers", "data", "list", 
                    "items", "result", "records"
                };
                
                for (String field : possibleArrayFields) {
                    if (rootNode.has(field) && rootNode.get(field).isArray()) {
                        // 找到数组字段，解析为试卷列表
                        JsonNode arrayNode = rootNode.get(field);
                        return mapper.convertValue(arrayNode, new TypeReference<List<ExamPaper>>() {});
                    }
                }
                
                // 如果没有找到数组字段，尝试直接解析为ExamPaperResponse
                if (rootNode.has("examPapers")) {
                    ExamPaperResponse response = mapper.treeToValue(rootNode, ExamPaperResponse.class);
                    return response.getExamPapers();
                }
            }
            
            log.warn("JSON格式不符合预期，返回空列表");
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("解析试卷数据失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 清理JSON字符串
     * @param jsonString JSON字符串
     * @return 清理后的JSON字符串
     */
    private String cleanJsonString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return "{}";
        }
        
        String cleaned = jsonString.trim();
        
        // 移除markdown代码块
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        // 移除首尾空白
        cleaned = cleaned.trim();
        
        // 如果字符串为空，返回空对象
        if (cleaned.isEmpty()) {
            return "{}";
        }
        
        return cleaned;
    }
    
    /**
     * 解析并处理试卷数据
     * @param jsonData JSON字符串
     * @return 处理结果
     */
    public Map<String, Object> processExamData(String jsonData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ExamPaper> examPapers = parseExamPapersFlexibly(jsonData);
            
            // 统计信息
            int totalPapers = examPapers.size();
            int totalQuestions = examPapers.stream()
                .mapToInt(ExamPaper::getQuestionCount)
                .sum();
            int totalScore = examPapers.stream()
                .mapToInt(ExamPaper::getTotalScore)
                .sum();
            
            // 按年份分组
            Map<String, List<ExamPaper>> papersByYear = examPapers.stream()
                .collect(Collectors.groupingBy(ExamPaper::getYear));
            
            // 按科目分组
            Map<String, List<ExamPaper>> papersBySubject = examPapers.stream()
                .collect(Collectors.groupingBy(ExamPaper::getSubjectName));
            
            result.put("success", true);
            result.put("examPapers", examPapers);
            result.put("totalPapers", totalPapers);
            result.put("totalQuestions", totalQuestions);
            result.put("totalScore", totalScore);
            result.put("papersByYear", papersByYear);
            result.put("papersBySubject", papersBySubject);
            
            log.info("成功处理 {} 份试卷，共 {} 题，总分 {}", 
                totalPapers, totalQuestions, totalScore);
            
        } catch (Exception e) {
            log.error("处理试卷数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("examPapers", new ArrayList<>());
        }
        
        return result;
    }
}

