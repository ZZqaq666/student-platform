package com.student.platform.service;

import com.student.platform.dto.CourseRecommendationDTO;

import java.util.List;

public interface CourseRecommendationService {
    List<CourseRecommendationDTO> getCourseRecommendations(Long userId);
}