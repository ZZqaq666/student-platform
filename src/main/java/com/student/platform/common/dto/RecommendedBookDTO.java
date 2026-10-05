package com.student.platform.common.dto;

import lombok.Data;

@Data
public class RecommendedBookDTO {

    private Long id;

    private String title;

    private String author;

    private String coverImage;

    private String description;

    private String major;

    private String semester;

    private String courseType;

    private String subject;

    private String recommendationReason;

    private Double relevanceScore;
}
