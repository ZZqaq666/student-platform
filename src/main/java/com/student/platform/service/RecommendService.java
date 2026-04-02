package com.student.platform.service;

import java.util.List;
import java.util.Map;

/**
 * 推荐服务接口
 */
public interface RecommendService {
    
    /**
     * 基于用户画像的试卷推荐
     */
    List<Map<String, Object>> recommendPapersByUserProfile(Long userId, String subjectId, Integer limit);
    
    /**
     * 基于协同过滤的试卷推荐
     */
    List<Map<String, Object>> recommendPapersByCollaborativeFiltering(Long userId, String subjectId, Integer limit);
    
    /**
     * 基于内容特征的试卷推荐
     */
    List<Map<String, Object>> recommendPapersByContentBased(Long userId, String subjectId, Integer limit);
    
    /**
     * 混合推荐算法
     */
    List<Map<String, Object>> recommendPapersByHybrid(Long userId, String subjectId, Integer limit);
    
    /**
     * 基于用户画像的题目推荐
     */
    List<Map<String, Object>> recommendQuestionsByUserProfile(Long userId, String subjectId, Integer limit);
    
    /**
     * 基于协同过滤的题目推荐
     */
    List<Map<String, Object>> recommendQuestionsByCollaborativeFiltering(Long userId, String subjectId, Integer limit);
    
    /**
     * 基于内容特征的题目推荐
     */
    List<Map<String, Object>> recommendQuestionsByContentBased(Long userId, String subjectId, Integer limit);
    
    /**
     * 混合题目推荐
     */
    List<Map<String, Object>> recommendQuestionsByHybrid(Long userId, String subjectId, Integer limit);
    
    /**
     * 计算用户相似度
     */
    Map<Long, Double> calculateUserSimilarity(Long userId);
    
    /**
     * 计算物品相似度
     */
    Map<String, Double> calculateItemSimilarity(String itemId, String itemType);
}
