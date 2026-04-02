package com.student.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户评价DTO
 * 包含用户评价的详细信息
 */
@Data
public class UserReviewDTO {
    
    /**
     * 评价ID
     */
    private Long id;
    
    /**
     * 书籍ID
     */
    private Long bookId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 评分（1-5星）
     */
    private Integer rating;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 评价时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 点赞数
     */
    private Integer likes;
}
