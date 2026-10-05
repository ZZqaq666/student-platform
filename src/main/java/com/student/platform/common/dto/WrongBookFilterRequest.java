package com.student.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrongBookFilterRequest {

    private String keyword;

    private String subject;

    private String difficulty;

    private String errorType;

    private String masterStatus;

    private String priority;

    private String tag;

    private Boolean showDueReview;

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private String sortBy = "created_at";

    private String sortDirection = "DESC";
}
