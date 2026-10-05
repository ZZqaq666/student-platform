package com.student.platform.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookRecommendationDTO {

    private List<RecommendedBookDTO> recommendedBooks;

    private List<UserReviewDTO> userReviews;
}
