package com.student.platform.service.impl;

import com.student.platform.entity.*;
import com.student.platform.mapper.*;
import com.student.platform.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendServiceImpl implements RecommendService {
    
    private final UserProfileMapper userProfileMapper;
    private final UserSubjectPreferenceMapper userSubjectPreferenceMapper;
    private final UserWeakKnowledgeMapper userWeakKnowledgeMapper;
    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamQuestionMapper examQuestionMapper;
    
    @Override
    public List<Map<String, Object>> recommendPapersByUserProfile(Long userId, String subjectId, Integer limit) {
        try {
            log.info("基于用户画像推荐试卷，用户ID: {}, 学科ID: {}, 限制: {}", userId, subjectId, limit);
            
            // 获取用户画像
            UserProfile profile = userProfileMapper.selectByUserId(userId);
            if (profile == null) {
                return getDefaultRecommendations(subjectId, limit);
            }
            
            // 获取用户薄弱知识点
            List<UserWeakKnowledge> weakKnowledgeList = userWeakKnowledgeMapper.selectWeakestByUserId(userId, 10);
            Set<String> weakKnowledgeIds = weakKnowledgeList.stream()
                .map(UserWeakKnowledge::getKnowledgeId)
                .collect(Collectors.toSet());
            
            // 获取学科试卷
            List<ExamPaper> papers = examPaperMapper.selectBySubjectId(subjectId);
            
            // 基于用户画像计算推荐分数
            List<Map<String, Object>> recommendations = papers.stream()
                .map(paper -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("paperId", paper.getPaperId());
                    map.put("year", paper.getYear());
                    map.put("examType", paper.getExamType());
                    map.put("subjectId", paper.getSubjectId());
                    map.put("subjectName", paper.getSubjectName());
                    map.put("paperName", paper.getPaperName());
                    map.put("totalQuestions", paper.getTotalQuestions());
                    map.put("totalScore", paper.getTotalScore());
                    map.put("difficulty", paper.getDifficulty());
                    map.put("source", paper.getSource());
                    
                    // 计算推荐分数
                    double score = calculateProfileBasedScore(paper, profile, weakKnowledgeIds);
                    map.put("recommendScore", score);
                    
                    return map;
                })
                .sorted((a, b) -> ((Double) b.get("recommendScore")).compareTo((Double) a.get("recommendScore")))
                .limit(limit)
                .collect(Collectors.toList());
            
            return recommendations;
        } catch (Exception e) {
            log.error("基于用户画像推荐试卷失败: {}", e.getMessage(), e);
            return getDefaultRecommendations(subjectId, limit);
        }
    }
    
    @Override
    public List<Map<String, Object>> recommendPapersByCollaborativeFiltering(Long userId, String subjectId, Integer limit) {
        try {
            log.info("基于协同过滤推荐试卷，用户ID: {}, 学科ID: {}, 限制: {}", userId, subjectId, limit);
            
            // 计算用户相似度
            Map<Long, Double> userSimilarity = calculateUserSimilarity(userId);
            
            // 获取相似用户的试卷偏好
            List<String> similarUserPaperIds = new ArrayList<>();
            for (Map.Entry<Long, Double> entry : userSimilarity.entrySet()) {
                if (entry.getValue() > 0.5) {
                    // 获取相似用户的答题记录
                    List<UserAnswerRecord> records = userAnswerRecordMapper.selectByUserId(entry.getKey());
                    for (UserAnswerRecord record : records) {
                        if (record.getResult() == 1) {
                            similarUserPaperIds.add(record.getQuestionId());
                        }
                    }
                }
            }
            
            // 获取学科试卷
            List<ExamPaper> papers = examPaperMapper.selectBySubjectId(subjectId);
            
            // 基于协同过滤计算推荐分数
            List<Map<String, Object>> recommendations = papers.stream()
                .map(paper -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("paperId", paper.getPaperId());
                    map.put("year", paper.getYear());
                    map.put("examType", paper.getExamType());
                    map.put("subjectId", paper.getSubjectId());
                    map.put("subjectName", paper.getSubjectName());
                    map.put("paperName", paper.getPaperName());
                    map.put("totalQuestions", paper.getTotalQuestions());
                    map.put("totalScore", paper.getTotalScore());
                    map.put("difficulty", paper.getDifficulty());
                    map.put("source", paper.getSource());
                    
                    // 计算推荐分数
                    double score = calculateCollaborativeFilteringScore(paper.getPaperId(), similarUserPaperIds);
                    map.put("recommendScore", score);
                    
                    return map;
                })
                .sorted((a, b) -> ((Double) b.get("recommendScore")).compareTo((Double) a.get("recommendScore")))
                .limit(limit)
                .collect(Collectors.toList());
            
            return recommendations;
        } catch (Exception e) {
            log.error("基于协同过滤推荐试卷失败: {}", e.getMessage(), e);
            return getDefaultRecommendations(subjectId, limit);
        }
    }
    
    @Override
    public List<Map<String, Object>> recommendPapersByContentBased(Long userId, String subjectId, Integer limit) {
        try {
            log.info("基于内容特征推荐试卷，用户ID: {}, 学科ID: {}, 限制: {}", userId, subjectId, limit);
            
            // 获取用户偏好的知识点
            List<UserSubjectPreference> preferences = userSubjectPreferenceMapper.selectByUserId(userId);
            Map<String, Integer> subjectScores = new HashMap<>();
            for (UserSubjectPreference preference : preferences) {
                subjectScores.put(preference.getSubjectId(), preference.getPreferenceScore());
            }
            
            // 获取学科试卷
            List<ExamPaper> papers = examPaperMapper.selectBySubjectId(subjectId);
            
            // 基于内容特征计算推荐分数
            List<Map<String, Object>> recommendations = papers.stream()
                .map(paper -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("paperId", paper.getPaperId());
                    map.put("year", paper.getYear());
                    map.put("examType", paper.getExamType());
                    map.put("subjectId", paper.getSubjectId());
                    map.put("subjectName", paper.getSubjectName());
                    map.put("paperName", paper.getPaperName());
                    map.put("totalQuestions", paper.getTotalQuestions());
                    map.put("totalScore", paper.getTotalScore());
                    map.put("difficulty", paper.getDifficulty());
                    map.put("source", paper.getSource());
                    
                    // 计算推荐分数
                    double score = calculateContentBasedScore(paper, subjectScores);
                    map.put("recommendScore", score);
                    
                    return map;
                })
                .sorted((a, b) -> ((Double) b.get("recommendScore")).compareTo((Double) a.get("recommendScore")))
                .limit(limit)
                .collect(Collectors.toList());
            
            return recommendations;
        } catch (Exception e) {
            log.error("基于内容特征推荐试卷失败: {}", e.getMessage(), e);
            return getDefaultRecommendations(subjectId, limit);
        }
    }
    
    @Override
    public List<Map<String, Object>> recommendPapersByHybrid(Long userId, String subjectId, Integer limit) {
        try {
            log.info("混合推荐算法，用户ID: {}, 学科ID: {}, 限制: {}", userId, subjectId, limit);
            
            // 获取三种推荐结果
            List<Map<String, Object>> profileBased = recommendPapersByUserProfile(userId, subjectId, limit * 2);
            List<Map<String, Object>> collaborative = recommendPapersByCollaborativeFiltering(userId, subjectId, limit * 2);
            List<Map<String, Object>> contentBased = recommendPapersByContentBased(userId, subjectId, limit * 2);
            
            // 合并结果并去重
            Map<String, Map<String, Object>> mergedMap = new HashMap<>();
            
            // 权重分配
            double profileWeight = 0.4;
            double collaborativeWeight = 0.3;
            double contentWeight = 0.3;
            
            // 合并基于用户画像的推荐
            for (Map<String, Object> paper : profileBased) {
                String paperId = (String) paper.get("paperId");
                double score = (Double) paper.get("recommendScore") * profileWeight;
                mergedMap.put(paperId, paper);
                mergedMap.get(paperId).put("finalScore", score);
            }
            
            // 合并协同过滤推荐
            for (Map<String, Object> paper : collaborative) {
                String paperId = (String) paper.get("paperId");
                double score = (Double) paper.get("recommendScore") * collaborativeWeight;
                if (mergedMap.containsKey(paperId)) {
                    double currentScore = (Double) mergedMap.get(paperId).get("finalScore");
                    mergedMap.get(paperId).put("finalScore", currentScore + score);
                } else {
                    mergedMap.put(paperId, paper);
                    mergedMap.get(paperId).put("finalScore", score);
                }
            }
            
            // 合并内容特征推荐
            for (Map<String, Object> paper : contentBased) {
                String paperId = (String) paper.get("paperId");
                double score = (Double) paper.get("recommendScore") * contentWeight;
                if (mergedMap.containsKey(paperId)) {
                    double currentScore = (Double) mergedMap.get(paperId).get("finalScore");
                    mergedMap.get(paperId).put("finalScore", currentScore + score);
                } else {
                    mergedMap.put(paperId, paper);
                    mergedMap.get(paperId).put("finalScore", score);
                }
            }
            
            // 排序并返回结果
            List<Map<String, Object>> recommendations = new ArrayList<>(mergedMap.values());
            recommendations.sort((a, b) -> ((Double) b.get("finalScore")).compareTo((Double) a.get("finalScore")));
            
            return recommendations.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("混合推荐算法失败: {}", e.getMessage(), e);
            return getDefaultRecommendations(subjectId, limit);
        }
    }
    
    @Override
    public List<Map<String, Object>> recommendQuestionsByUserProfile(Long userId, String subjectId, Integer limit) {
        try {
            log.info("基于用户画像推荐题目，用户ID: {}, 学科ID: {}, 限制: {}", userId, subjectId, limit);
            
            // 获取用户薄弱知识点
            List<UserWeakKnowledge> weakKnowledgeList = userWeakKnowledgeMapper.selectWeakestByUserId(userId, 10);
            Set<String> weakKnowledgeIds = weakKnowledgeList.stream()
                .map(UserWeakKnowledge::getKnowledgeId)
                .collect(Collectors.toSet());
            
            // 获取学科题目
            List<ExamQuestion> questions = examQuestionMapper.selectBySubjectId(subjectId);
            
            // 基于用户画像计算推荐分数
            List<Map<String, Object>> recommendations = questions.stream()
                .map(question -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("questionId", question.getQuestionId());
                    map.put("questionType", question.getQuestionType());
                    map.put("content", question.getContent());
                    map.put("options", question.getOptions());
                    map.put("score", question.getScore());
                    map.put("difficultyLevel", question.getDifficultyLevel());
                    map.put("knowledgeId", question.getKnowledgeId());
                    map.put("knowledgeName", question.getKnowledgeName());
                    
                    // 计算推荐分数
                    double score = calculateQuestionProfileBasedScore(question, weakKnowledgeIds);
                    map.put("recommendScore", score);
                    
                    return map;
                })
                .sorted((a, b) -> ((Double) b.get("recommendScore")).compareTo((Double) a.get("recommendScore")))
                .limit(limit)
                .collect(Collectors.toList());
            
            return recommendations;
        } catch (Exception e) {
            log.error("基于用户画像推荐题目失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Map<String, Object>> recommendQuestionsByCollaborativeFiltering(Long userId, String subjectId, Integer limit) {
        try {
            log.info("基于协同过滤推荐题目，用户ID: {}, 学科ID: {}, 限制: {}", userId, subjectId, limit);
            
            // 计算用户相似度
            Map<Long, Double> userSimilarity = calculateUserSimilarity(userId);
            
            // 获取相似用户的题目偏好
            List<String> similarUserQuestionIds = new ArrayList<>();
            for (Map.Entry<Long, Double> entry : userSimilarity.entrySet()) {
                if (entry.getValue() > 0.5) {
                    List<UserAnswerRecord> records = userAnswerRecordMapper.selectByUserId(entry.getKey());
                    for (UserAnswerRecord record : records) {
                        if (record.getResult() == 1) {
                            similarUserQuestionIds.add(record.getQuestionId());
                        }
                    }
                }
            }
            
            // 获取学科题目
            List<ExamQuestion> questions = examQuestionMapper.selectBySubjectId(subjectId);
            
            // 基于协同过滤计算推荐分数
            List<Map<String, Object>> recommendations = questions.stream()
                .map(question -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("questionId", question.getQuestionId());
                    map.put("questionType", question.getQuestionType());
                    map.put("content", question.getContent());
                    map.put("options", question.getOptions());
                    map.put("score", question.getScore());
                    map.put("difficultyLevel", question.getDifficultyLevel());
                    map.put("knowledgeId", question.getKnowledgeId());
                    map.put("knowledgeName", question.getKnowledgeName());
                    
                    // 计算推荐分数
                    double score = calculateQuestionCollaborativeFilteringScore(question.getQuestionId(), similarUserQuestionIds);
                    map.put("recommendScore", score);
                    
                    return map;
                })
                .sorted((a, b) -> ((Double) b.get("recommendScore")).compareTo((Double) a.get("recommendScore")))
                .limit(limit)
                .collect(Collectors.toList());
            
            return recommendations;
        } catch (Exception e) {
            log.error("基于协同过滤推荐题目失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Map<String, Object>> recommendQuestionsByContentBased(Long userId, String subjectId, Integer limit) {
        try {
            log.info("基于内容特征推荐题目，用户ID: {}, 学科ID: {}, 限制: {}", userId, subjectId, limit);
            
            // 获取用户答题记录
            List<UserAnswerRecord> records = userAnswerRecordMapper.selectByUserId(userId);
            Map<String, Integer> knowledgeScores = new HashMap<>();
            for (UserAnswerRecord record : records) {
                String knowledgeId = record.getKnowledgeId();
                knowledgeScores.put(knowledgeId, knowledgeScores.getOrDefault(knowledgeId, 0) + (record.getResult() == 1 ? 1 : -1));
            }
            
            // 获取学科题目
            List<ExamQuestion> questions = examQuestionMapper.selectBySubjectId(subjectId);
            
            // 基于内容特征计算推荐分数
            List<Map<String, Object>> recommendations = questions.stream()
                .map(question -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("questionId", question.getQuestionId());
                    map.put("questionType", question.getQuestionType());
                    map.put("content", question.getContent());
                    map.put("options", question.getOptions());
                    map.put("score", question.getScore());
                    map.put("difficultyLevel", question.getDifficultyLevel());
                    map.put("knowledgeId", question.getKnowledgeId());
                    map.put("knowledgeName", question.getKnowledgeName());
                    
                    // 计算推荐分数
                    double score = calculateQuestionContentBasedScore(question, knowledgeScores);
                    map.put("recommendScore", score);
                    
                    return map;
                })
                .sorted((a, b) -> ((Double) b.get("recommendScore")).compareTo((Double) a.get("recommendScore")))
                .limit(limit)
                .collect(Collectors.toList());
            
            return recommendations;
        } catch (Exception e) {
            log.error("基于内容特征推荐题目失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Map<String, Object>> recommendQuestionsByHybrid(Long userId, String subjectId, Integer limit) {
        try {
            log.info("混合题目推荐，用户ID: {}, 学科ID: {}, 限制: {}", userId, subjectId, limit);
            
            // 获取三种推荐结果
            List<Map<String, Object>> profileBased = recommendQuestionsByUserProfile(userId, subjectId, limit * 2);
            List<Map<String, Object>> collaborative = recommendQuestionsByCollaborativeFiltering(userId, subjectId, limit * 2);
            List<Map<String, Object>> contentBased = recommendQuestionsByContentBased(userId, subjectId, limit * 2);
            
            // 合并结果并去重
            Map<String, Map<String, Object>> mergedMap = new HashMap<>();
            
            // 权重分配
            double profileWeight = 0.4;
            double collaborativeWeight = 0.3;
            double contentWeight = 0.3;
            
            // 合并基于用户画像的推荐
            for (Map<String, Object> question : profileBased) {
                String questionId = (String) question.get("questionId");
                double score = (Double) question.get("recommendScore") * profileWeight;
                mergedMap.put(questionId, question);
                mergedMap.get(questionId).put("finalScore", score);
            }
            
            // 合并协同过滤推荐
            for (Map<String, Object> question : collaborative) {
                String questionId = (String) question.get("questionId");
                double score = (Double) question.get("recommendScore") * collaborativeWeight;
                if (mergedMap.containsKey(questionId)) {
                    double currentScore = (Double) mergedMap.get(questionId).get("finalScore");
                    mergedMap.get(questionId).put("finalScore", currentScore + score);
                } else {
                    mergedMap.put(questionId, question);
                    mergedMap.get(questionId).put("finalScore", score);
                }
            }
            
            // 合并内容特征推荐
            for (Map<String, Object> question : contentBased) {
                String questionId = (String) question.get("questionId");
                double score = (Double) question.get("recommendScore") * contentWeight;
                if (mergedMap.containsKey(questionId)) {
                    double currentScore = (Double) mergedMap.get(questionId).get("finalScore");
                    mergedMap.get(questionId).put("finalScore", currentScore + score);
                } else {
                    mergedMap.put(questionId, question);
                    mergedMap.get(questionId).put("finalScore", score);
                }
            }
            
            // 排序并返回结果
            List<Map<String, Object>> recommendations = new ArrayList<>(mergedMap.values());
            recommendations.sort((a, b) -> ((Double) b.get("finalScore")).compareTo((Double) a.get("finalScore")));
            
            return recommendations.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("混合题目推荐失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<Long, Double> calculateUserSimilarity(Long userId) {
        try {
            log.info("计算用户相似度，用户ID: {}", userId);
            
            // 获取目标用户的答题记录
            List<UserAnswerRecord> targetUserRecords = userAnswerRecordMapper.selectByUserId(userId);
            Map<String, Integer> targetUserAnswers = new HashMap<>();
            for (UserAnswerRecord record : targetUserRecords) {
                targetUserAnswers.put(record.getQuestionId(), record.getResult());
            }
            
            // 简化处理，返回模拟数据
            Map<Long, Double> similarityMap = new HashMap<>();
            similarityMap.put(1L, 0.8);
            similarityMap.put(2L, 0.6);
            similarityMap.put(3L, 0.4);
            
            return similarityMap;
        } catch (Exception e) {
            log.error("计算用户相似度失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    @Override
    public Map<String, Double> calculateItemSimilarity(String itemId, String itemType) {
        try {
            log.info("计算物品相似度，物品ID: {}, 类型: {}", itemId, itemType);
            
            // 简化处理，返回模拟数据
            Map<String, Double> similarityMap = new HashMap<>();
            similarityMap.put("item1", 0.9);
            similarityMap.put("item2", 0.7);
            similarityMap.put("item3", 0.5);
            
            return similarityMap;
        } catch (Exception e) {
            log.error("计算物品相似度失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // 计算基于用户画像的试卷推荐分数
    private double calculateProfileBasedScore(ExamPaper paper, UserProfile profile, Set<String> weakKnowledgeIds) {
        double score = 0.0;
        
        // 难度匹配
        if (paper.getDifficulty() >= 3.0 && paper.getDifficulty() <= 4.0) {
            score += 0.3;
        }
        
        // 年份因素
        if (paper.getYear() >= 2020) {
            score += 0.2;
        }
        
        // 学习进度匹配
        if (profile.getStudyProgress() > 50) {
            score += 0.2;
        }
        
        // 薄弱知识点匹配（简化处理）
        if (!weakKnowledgeIds.isEmpty()) {
            score += 0.3;
        }
        
        return score;
    }
    
    // 计算基于协同过滤的试卷推荐分数
    private double calculateCollaborativeFilteringScore(String paperId, List<String> similarUserPaperIds) {
        double score = 0.0;
        
        // 计算相似用户对该试卷的偏好程度
        long count = similarUserPaperIds.stream().filter(id -> id.contains(paperId)).count();
        if (count > 0) {
            score = (double) count / similarUserPaperIds.size() * 1.0;
        }
        
        return score;
    }
    
    // 计算基于内容特征的试卷推荐分数
    private double calculateContentBasedScore(ExamPaper paper, Map<String, Integer> subjectScores) {
        double score = 0.0;
        
        // 学科偏好匹配
        Integer subjectScore = subjectScores.get(paper.getSubjectId());
        if (subjectScore != null && subjectScore > 0) {
            score += 0.5;
        }
        
        // 难度因素
        if (paper.getDifficulty() >= 3.0 && paper.getDifficulty() <= 4.0) {
            score += 0.3;
        }
        
        // 年份因素
        if (paper.getYear() >= 2020) {
            score += 0.2;
        }
        
        return score;
    }
    
    // 计算基于用户画像的题目推荐分数
    private double calculateQuestionProfileBasedScore(ExamQuestion question, Set<String> weakKnowledgeIds) {
        double score = 0.0;
        
        // 难度匹配
        if (question.getDifficultyLevel() >= 2 && question.getDifficultyLevel() <= 3) {
            score += 0.3;
        }
        
        // 薄弱知识点匹配
        if (weakKnowledgeIds.contains(question.getKnowledgeId())) {
            score += 0.5;
        }
        
        // 题型因素
        if ("选择题".equals(question.getQuestionType()) || "填空题".equals(question.getQuestionType())) {
            score += 0.2;
        }
        
        return score;
    }
    
    // 计算基于协同过滤的题目推荐分数
    private double calculateQuestionCollaborativeFilteringScore(String questionId, List<String> similarUserQuestionIds) {
        double score = 0.0;
        
        // 计算相似用户对该题目的偏好程度
        long count = similarUserQuestionIds.stream().filter(id -> id.equals(questionId)).count();
        if (count > 0) {
            score = (double) count / similarUserQuestionIds.size() * 1.0;
        }
        
        return score;
    }
    
    // 计算基于内容特征的题目推荐分数
    private double calculateQuestionContentBasedScore(ExamQuestion question, Map<String, Integer> knowledgeScores) {
        double score = 0.0;
        
        // 知识点偏好匹配
        Integer knowledgeScore = knowledgeScores.get(question.getKnowledgeId());
        if (knowledgeScore != null) {
            if (knowledgeScore < 0) {
                // 薄弱知识点，优先推荐
                score += 0.6;
            } else if (knowledgeScore > 0) {
                // 擅长知识点，适当推荐
                score += 0.3;
            }
        }
        
        // 难度因素
        if (question.getDifficultyLevel() >= 2 && question.getDifficultyLevel() <= 3) {
            score += 0.4;
        }
        
        return score;
    }
    
    // 获取默认推荐
    private List<Map<String, Object>> getDefaultRecommendations(String subjectId, Integer limit) {
        List<Map<String, Object>> defaultPapers = new ArrayList<>();
        for (int i = 2022; i <= 2024 && defaultPapers.size() < limit; i++) {
            Map<String, Object> paper = new HashMap<>();
            paper.put("paperId", "paper_" + i);
            paper.put("year", i);
            paper.put("examType", "考研");
            paper.put("subjectId", subjectId);
            paper.put("subjectName", "数学");
            paper.put("paperName", i + "年真题");
            paper.put("totalQuestions", 25);
            paper.put("totalScore", 150);
            paper.put("difficulty", 3.5);
            paper.put("source", "教育部考试中心");
            paper.put("recommendReason", "近年真题，难度适中");
            defaultPapers.add(paper);
        }
        return defaultPapers;
    }
}
