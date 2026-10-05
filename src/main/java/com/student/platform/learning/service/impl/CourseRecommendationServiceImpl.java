package com.student.platform.learning.service.impl;

import com.student.platform.common.dto.CourseRecommendationDTO;
import com.student.platform.learning.service.CourseRecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CourseRecommendationServiceImpl implements CourseRecommendationService {

    @Override
    public List<CourseRecommendationDTO> getCourseRecommendations(Long userId) {
        log.warn("课程推荐服务暂不可用（需依赖问答服务数据），用户ID：{}", userId);
        return new ArrayList<>();
    }
}
