package com.student.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaResponse {
    private Long id;
    private String question;
    private String answer;
    private String subject;
    private Long bookId;
    private Long knowledgeNodeId;
    private LocalDateTime createdAt;
}
