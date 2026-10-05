package com.student.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseStatsDTO {
    private Long totalExercises;
    private Long attemptedExercises;
    private Long correctCount;
    private Long wrongCount;
    private Double accuracyRate;
    private Integer totalScore;
    private Integer obtainedScore;
}
