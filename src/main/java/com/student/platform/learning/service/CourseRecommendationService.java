package com.student.platform.learning.service;

import com.student.platform.common.dto.CourseRecommendationDTO;

import java.util.List;

public interface CourseRecommendationService {

    List<CourseRecommendationDTO> getCourseRecommendations(Long userId);
}
