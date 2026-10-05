package com.student.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrongBookDTO {
    private Long id;
    private Long userId;
    private ExerciseDTO exercise;
    private Long answerRecordId;
    private Integer wrongCount;
    private Integer correctCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAttemptAt;

    private String masterStatus;

    private String notes;

    private String tags;

    private String errorType;

    private String subject;

    private String difficulty;

    private String priority;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextReviewAt;

    private Integer reviewCount;

    private String userAnswer;

    private String correctAnswer;

    private String errorAnalysis;

    private Boolean isReviewed;

    private Integer daysUntilReview;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
