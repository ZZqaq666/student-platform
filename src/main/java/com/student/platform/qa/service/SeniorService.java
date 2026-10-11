package com.student.platform.qa.service;

import java.util.List;
import java.util.Map;

public interface SeniorService {

    // 获取统计数据
    Map<String, Object> getStats();

    // 获取所有学长学姐（online 为按最近活跃时间动态计算，不再是写死的种子值）
    List<Map<String, Object>> getSeniors();

    // 获取学长学姐详情
    Map<String, Object> getSeniorById(String id);

    /**
     * 获取问题列表（分页）。
     *
     * @param seniorId      指定学长人设过滤（null 表示全部）
     * @param category      学科分类过滤（null/all 表示全部）
     * @param filter        状态过滤：all|hot|unsolved|solved
     * @param sort          排序：latest|likes
     * @param currentUserId 当前登录用户，用于计算「我是否点过赞」
     * @return {list, total, page, pageSize}
     */
    Map<String, Object> getQuestions(String seniorId, String category, String filter, String sort,
                                     int page, int pageSize, Long currentUserId);

    /**
     * 获取问题详情，并累加浏览量。
     */
    Map<String, Object> getQuestionById(String id, Long currentUserId);

    // 发布问题（绑定真实登录用户），并异步触发学长回答生成
    Map<String, Object> publishQuestion(Map<String, Object> data, Long userId, String displayName);

    /**
     * 点赞/取消点赞回答（按用户维度）。
     *
     * @return {liked, likes}
     */
    Map<String, Object> likeAnswer(String answerId, Long userId);

    /**
     * 采纳回答：校验提问人身份与回答归属。
     */
    void acceptAnswer(String questionId, String answerId, Long userId);

    /**
     * 提交追问（绑定真实登录用户），并异步触发同一位学长回复。
     */
    Map<String, Object> submitFollowUp(String questionId, String content, Long userId, String displayName);

    /**
     * 重新生成回答：AI 生成失败后由提问者主动重试。
     */
    Map<String, Object> regenerateAnswer(String questionId, Long userId);
}
