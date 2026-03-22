package com.student.platform.service;

import java.util.List;
import java.util.Map;

public interface SeniorService {

    // 获取所有学长学姐
    List<Map<String, Object>> getSeniors();

    // 获取学长学姐详情
    Map<String, Object> getSeniorById(String id);

    // 获取问题列表
    List<Map<String, Object>> getQuestions(String seniorId, String filter, String sort, int page, int pageSize);

    // 获取问题详情
    Map<String, Object> getQuestionById(String id);

    // 发布问题
    Map<String, Object> publishQuestion(Map<String, Object> data);

    // 点赞回答
    void likeAnswer(String answerId);

    // 采纳回答
    void acceptAnswer(String questionId, String answerId);

    // 提交追问
    void submitFollowUp(String questionId, String content);
}
