package com.student.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgressRequest {
    private Long bookId;
    private BigDecimal progress;
    private Integer pagesRead;
    private Integer studyTimeMinutes;
}
