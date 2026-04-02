package com.student.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 笔记DTO
 */
@Data
public class NoteDTO {
    /**
     * 笔记ID
     */
    private Long id;
    
    /**
     * 书籍ID
     */
    private Long bookId;
    
    /**
     * 章节ID
     */
    private Long chapterId;
    
    /**
     * 笔记标题
     */
    private String title;
    
    /**
     * 笔记内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
