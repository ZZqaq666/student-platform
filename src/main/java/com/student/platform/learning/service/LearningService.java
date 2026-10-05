package com.student.platform.learning.service;

import com.student.platform.common.dto.LearningCurveDTO;
import com.student.platform.learning.entity.LearningProgress;

import java.time.LocalDate;
import java.util.List;

public interface LearningService {

    LearningCurveDTO getLearningCurve(Long userId, Long bookId, LocalDate startDate, LocalDate endDate);

    LearningProgress saveLearningProgress(LearningProgress learningProgress);

    List<LearningProgress> getLearningHistory(Long userId, LocalDate startDate, LocalDate endDate);
}
