package com.student.platform.controller;

import com.student.platform.dto.CourseRecommendationDTO;
import com.student.platform.dto.Result;
import com.student.platform.service.CourseRecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/course-recommendations")
@Slf4j
public class CourseRecommendationController {

    private final CourseRecommendationService courseRecommendationService;

    @Autowired
    public CourseRecommendationController(CourseRecommendationService courseRecommendationService) {
        this.courseRecommendationService = courseRecommendationService;
    }

    @GetMapping
    public Result<List<CourseRecommendationDTO>> getCourseRecommendations(HttpServletRequest request) {
        try {
            // 从请求属性（由 UserIdInterceptor 设置）获取用户 ID。
            Long userId = (Long) request.getAttribute("userId");
            
            if (userId == null) {
                log.warn("请求中找不到用户ID。");
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户未登录");
            }
            
            List<CourseRecommendationDTO> recommendations = courseRecommendationService.getCourseRecommendations(userId);
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("获取课程推荐时出现错误: {}", e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取课程推荐失败");
        }
    }
}