package com.student.platform.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface CourseRecommendService {
    
    List<Map<String, Object>> recommendCoursesByQuestionHistory(Long userId, Integer limit);
    
    CompletableFuture<List<Map<String, Object>>> recommendCoursesByQuestionHistoryAsync(Long userId, Integer limit);
    
    List<Map<String, Object>> recommendCoursesByExamQaHistory(Long userId, Integer limit);
    
    CompletableFuture<List<Map<String, Object>>> recommendCoursesByExamQaHistoryAsync(Long userId, Integer limit);
    
    List<Map<String, Object>> recommendCoursesByKnowledge(List<String> knowledgePoints, Integer limit);
    
    List<Map<String, Object>> recommendCoursesBySubject(String subject, Integer limit);
}
