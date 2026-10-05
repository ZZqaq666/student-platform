package com.student.platform.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserReviewDTO {

    private Long id;

    private Long bookId;

    private Long userId;

    private String username;

    private Integer rating;

    private String content;

    private LocalDateTime createdAt;

    private Integer likes;
}
