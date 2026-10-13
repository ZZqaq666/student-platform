package com.student.platform.learning.controller;

import com.student.platform.common.config.CurrentUserProvider;
import com.student.platform.common.dto.LearningCurveDTO;
import com.student.platform.common.dto.LearningProgressRequest;
import com.student.platform.common.result.Result;
import com.student.platform.learning.entity.LearningProgress;
import com.student.platform.learning.service.LearningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/learning")
@RequiredArgsConstructor
@Tag(name = "学习管理", description = "学习进度和曲线相关接口")
public class LearningController {

    private final LearningService learningService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/curve")
    @Operation(summary = "获取学习曲线数据", description = "根据用户ID和书籍ID获取学习曲线数据，支持指定日期范围")
    public Result<LearningCurveDTO> getLearningCurve(
            @Parameter(description = "书籍ID", required = true) @RequestParam Long bookId,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", example = "2026-03-01")
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", example = "2026-03-21")
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate endDate
    ) {
        Long userId = currentUserProvider.require().id();
        return Result.success(learningService.getLearningCurve(userId, bookId, startDate, endDate));
    }

    @GetMapping("/history")
    @Operation(summary = "获取学习历史数据", description = "获取当前用户的学习历史数据，支持指定日期范围")
    public Result<List<LearningProgress>> getLearningHistory(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", example = "2026-03-01")
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", example = "2026-03-21")
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate endDate
    ) {
        Long userId = currentUserProvider.require().id();
        return Result.success(learningService.getLearningHistory(userId, startDate, endDate));
    }

    @PostMapping("/progress")
    @Operation(summary = "保存学习进度", description = "保存当前用户的学习进度")
    public Result<LearningProgress> saveProgress(
            @Parameter(description = "学习进度数据", required = true) @RequestBody LearningProgressRequest request
    ) {
        Long userId = currentUserProvider.require().id();

        LearningProgress learningProgress = new LearningProgress();
        learningProgress.setUserId(userId);
        learningProgress.setBookId(request.getBookId());
        learningProgress.setDate(LocalDate.now());
        learningProgress.setProgress(request.getProgress());
        learningProgress.setPagesRead(request.getPagesRead());
        learningProgress.setStudyTimeMinutes(request.getStudyTimeMinutes());
        learningProgress.setCreatedAt(LocalDateTime.now());
        learningProgress.setUpdatedAt(LocalDateTime.now());

        return Result.success(learningService.saveLearningProgress(learningProgress));
    }
}
