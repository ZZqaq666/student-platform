package com.student.platform.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoteDTO {
    private Long id;
    /** 所属用户（后端真实化后新增，前端无感知） */
    private Long userId;
    private Long bookId;
    private Long chapterId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
