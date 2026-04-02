package com.student.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String isbn;
    private String coverImage;
    private String description;
    private String subject;
    private String grade;
    private String version;
    private String status;
    private String category;
    private String courseType;
    private String major;
    private String semester;
    private String universityLevel;
    
    private String createdAt;
    
    private String updatedAt;
}
