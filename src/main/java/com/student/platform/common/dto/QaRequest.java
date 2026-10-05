package com.student.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaRequest {

    private String question;

    private String subject;

    private Long bookId;

    private Long knowledgeNodeId;

    private Long userId;

    private String sessionId;
}
