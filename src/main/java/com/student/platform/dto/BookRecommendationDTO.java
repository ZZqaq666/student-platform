package com.student.platform.dto;

import lombok.Data;
import java.util.List;

/**
 * 书籍推荐DTO
 * 包含推荐书籍列表和用户评价信息
 */
@Data
public class BookRecommendationDTO {
    
    /**
     * 推荐书籍列表
     */
    private List<RecommendedBookDTO> recommendedBooks;
    
    /**
     * 用户评价列表
     */
    private List<UserReviewDTO> userReviews;
}
