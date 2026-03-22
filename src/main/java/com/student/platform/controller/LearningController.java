package com.student.platform.controller;

import com.student.platform.dto.LearningCurveDTO;
import com.student.platform.dto.Result;
import com.student.platform.entity.LearningProgress;
import com.student.platform.entity.User;
import com.student.platform.mapper.UserMapper;
import com.student.platform.service.LearningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/learning")
@RequiredArgsConstructor
@Tag(name = "学习管理", description = "学习进度和曲线相关接口")
public class LearningController {
    
    private final LearningService learningService;
    
    @Autowired
    private UserMapper userMapper;
    
    @GetMapping("/curve")
    @Operation(summary = "获取学习曲线数据", description = "根据用户ID和书籍ID获取学习曲线数据，支持指定日期范围")
    public Result<LearningCurveDTO> getLearningCurve(
            Authentication authentication,
            @Parameter(description = "书籍ID", required = true) @RequestParam Long bookId,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", example = "2026-03-01") 
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", example = "2026-03-21") 
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate endDate
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        Long userId = user.getId();
        
        LearningCurveDTO learningCurve = learningService.getLearningCurve(userId, bookId, startDate, endDate);
        
        return Result.success(learningCurve);
    }

    @GetMapping("/history")
    @Operation(summary = "获取学习历史数据", description = "获取当前用户的学习历史数据，支持指定日期范围")
    public Result<List<LearningProgress>> getLearningHistory(
            Authentication authentication,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", example = "2026-03-01") 
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", example = "2026-03-21") 
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate endDate
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.success(List.of());
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return Result.success(List.of());
        }
        
        UserDetails userDetails = (UserDetails) principal;
        String username = userDetails.getUsername();
        
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.success(List.of());
        }
        
        Long userId = user.getId();
        
        List<LearningProgress> history = learningService.getLearningHistory(userId, startDate, endDate);
        
        return Result.success(history);
    }
}
