package com.student.platform.service;

import com.student.platform.entity.BookKnowledgePoint;

import java.util.List;

public interface BookKnowledgePointService {
    
    List<String> getRelatedKnowledgePoints(Long bookId);
    
    List<String> getHighFrequencyKnowledgePoints(Long bookId);
    
    void saveKnowledgePoints(Long bookId, List<String> relatedPoints, List<String> highFrequencyPoints);
}
