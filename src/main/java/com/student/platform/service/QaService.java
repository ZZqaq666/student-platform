package com.student.platform.service;

import com.student.platform.dto.QaRequest;
import com.student.platform.dto.QaResponse;

import java.util.List;
import java.util.Map;

public interface QaService {
    QaResponse askQuestion(QaRequest request);
    List<QaResponse> getHistory();
    void deleteHistory(Long id);
    List<Map<String, Object>> getExamSubjects();
    List<Map<String, Object>> getKeyPoints(String subjectId);
    List<Map<String, Object>> getExamHistory();
    List<Map<String, Object>> getBooks();
    List<Map<String, Object>> getCourses();
}