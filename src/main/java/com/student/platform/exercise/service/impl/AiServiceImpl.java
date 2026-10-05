package com.student.platform.exercise.service.impl;

import com.student.platform.exercise.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiServiceImpl implements AiService {
    @Override
    public String generateExercises(String knowledgePoint, String difficulty) {
        log.warn("AiServiceImpl.generateExercises called - stub implementation, knowledgePoint: {}, difficulty: {}", knowledgePoint, difficulty);
        return "{\"exercises\": []}";
    }
}
