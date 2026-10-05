package com.student.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaSessionDTO {

    private String sessionId;

    private String title;

    private List<QaMessageDTO> messages;

    private Long bookId;

    private String context;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QaMessageDTO {
        private Long id;
        private String question;
        private String answer;
        private LocalDateTime createdAt;
    }
}
