package com.student.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseRecommendationDTO {
    private String title;
    private String url;
    private String description;
    private String platform;
    private String level;
    private double rating;
    private int reviewCount;
    private String imageUrl;
    private String category;
}