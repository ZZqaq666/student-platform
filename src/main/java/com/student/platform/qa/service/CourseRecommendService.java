package com.student.platform.qa.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 课程推荐服务接口
 *
 * 功能说明：
 * - 根据用户行为数据推荐相关课程
 * - 支持多种推荐策略（问答历史、知识点、学科等）
 * - 提供同步和异步两种调用方式
 *
 * 推荐策略：
 * - 基于问答历史推荐
 * - 基于考试问答历史推荐
 * - 基于知识点推荐
 * - 基于学科推荐
 *
 * 返回数据结构：
 * - courseId: 课程ID
 * - courseName: 课程名称
 * - subject: 学科
 * - score: 推荐分数
 * - description: 课程描述
 *
 * @author student-platform
 * @since 1.0.0
 */
public interface CourseRecommendService {
    
    /**
     * 根据问答历史推荐课程
     *
     * @param userId 用户ID
     * @param limit  返回数量限制
     * @return 推荐课程列表
     */
    List<Map<String, Object>> recommendCoursesByQuestionHistory(Long userId, Integer limit);
    
    /**
     * 异步根据问答历史推荐课程
     *
     * @param userId 用户ID
     * @param limit  返回数量限制
     * @return CompletableFuture包含推荐课程列表
     */
    CompletableFuture<List<Map<String, Object>>> recommendCoursesByQuestionHistoryAsync(Long userId, Integer limit);
    
    /**
     * 根据考试问答历史推荐课程
     *
     * @param userId 用户ID
     * @param limit  返回数量限制
     * @return 推荐课程列表
     */
    List<Map<String, Object>> recommendCoursesByExamQaHistory(Long userId, Integer limit);
    
    /**
     * 异步根据考试问答历史推荐课程
     *
     * @param userId 用户ID
     * @param limit  返回数量限制
     * @return CompletableFuture包含推荐课程列表
     */
    CompletableFuture<List<Map<String, Object>>> recommendCoursesByExamQaHistoryAsync(Long userId, Integer limit);
    
    /**
     * 根据知识点推荐课程
     *
     * @param knowledgePoints 知识点列表
     * @param limit           返回数量限制
     * @return 推荐课程列表
     */
    List<Map<String, Object>> recommendCoursesByKnowledge(List<String> knowledgePoints, Integer limit);
    
    /**
     * 根据学科推荐课程
     *
     * @param subject 学科名称
     * @param limit   返回数量限制
     * @return 推荐课程列表
     */
    List<Map<String, Object>> recommendCoursesBySubject(String subject, Integer limit);
}
