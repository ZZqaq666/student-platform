package com.student.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrongBookStatsDTO {

    private Integer totalWrongCount;

    private Integer masteredCount;

    private Integer masteringCount;

    private Integer notMasteredCount;

    private Integer dueTodayCount;

    private Integer dueThisWeekCount;

    private Map<String, Integer> subjectDistribution;

    private Map<String, Integer> difficultyDistribution;

    private Map<String, Integer> errorTypeDistribution;

    private Map<String, Integer> masterStatusDistribution;

    private Map<String, Integer> weeklyTrend;

    private Map<String, Integer> monthlyTrend;

    private Double averageReviewCount;

    private Double averageMasterDays;
}
