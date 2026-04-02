package com.student.platform.dto;

import lombok.Data;

/**
 * 推荐书籍DTO
 * 包含推荐书籍的详细信息
 */
@Data
public class RecommendedBookDTO {
    
    /**
     * 书籍ID
     */
    private Long id;
    
    /**
     * 书名
     */
    private String title;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 封面图URL
     */
    private String coverImage;
    
    /**
     * 简介
     */
    private String description;
    
    /**
     * 专业领域
     */
    private String major;
    
    /**
     * 学期
     */
    private String semester;
    
    /**
     * 课程类型
     */
    private String courseType;
    
    /**
     * 学科
     */
    private String subject;
    
    /**
     * 推荐理由
     */
    private String recommendationReason;
    
    /**
     * 相关度分数
     */
    private Double relevanceScore;
}
