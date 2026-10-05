package com.student.platform.qa.service;

import com.student.platform.common.dto.CourseRecommendationDTO;

import java.util.List;

/**
 * 课程推荐服务接口（简化版）
 *
 * 功能说明：
 * - 根据用户ID获取个性化课程推荐
 * - 返回结构化的课程推荐数据
 *
 * 使用场景：
 * - 用户首页课程推荐展示
 * - 学习路径规划
 * - 个性化学习建议
 *
 * @author student-platform
 * @since 1.0.0
 */
public interface CourseRecommendationService {
    
    /**
     * 获取用户的课程推荐列表
     *
     * @param userId 用户ID
     * @return 课程推荐DTO列表
     */
    List<CourseRecommendationDTO> getCourseRecommendations(Long userId);
}