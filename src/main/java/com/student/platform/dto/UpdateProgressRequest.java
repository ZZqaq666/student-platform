package com.student.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgressRequest {
    private BigDecimal progress;
    private Integer lastReadPage;
    private Long lastReadChapterId;
}
