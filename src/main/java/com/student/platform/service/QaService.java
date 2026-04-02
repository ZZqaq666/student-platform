package com.student.platform.service;

import com.student.platform.dto.QaRequest;
import com.student.platform.dto.QaResponse;
import com.student.platform.entity.UserProfile;

import java.util.List;
import java.util.Map;

public interface QaService {
    List<QaResponse> getHistory(Long userId);
    void deleteHistory(Long id);
    List<Map<String, Object>> getExamSubjects();
    List<Map<String, Object>> getKeyPoints(String subjectId);
    List<Map<String, Object>> getExamPapers(String subjectId);
    List<Map<String, Object>> generateExamPapersByAI(String subjectId);
    List<Map<String, Object>> getExamPaperQuestions(String paperId);
    List<Map<String, Object>> getExamHistory();
    List<Map<String, Object>> getBooks();
    List<Map<String, Object>> getCourses();
    void saveProcessedText(String question, String processedText, String subject, Long userId);

    // 考试准备Q&A历史记录方法
    List<Map<String, Object>> getExamQaHistory(Long userId);
    void deleteExamQaHistory(Long id);
    void saveExamQaHistory(String question, String answer, String subject, Long userId);

    // 课程推荐方法
    Map<String, Object> getCourseRecommendations();
    
    // 获取指定科目的课程
    List<Map<String, Object>> getExamCourses(String subjectId);

    // 获取指定书籍的章节列表
    List<Map<String, Object>> getChapters(Long bookId);
}