package com.student.platform.dto;

import com.student.platform.entity.BookChapter;
import lombok.Data;

import java.util.List;

@Data
public class BookChapterTreeDTO {
    
    private Long id;
    private Long bookId;
    private String title;
    private Integer sortOrder;
    private Long parentId;
    private String status;
    private List<BookChapterTreeDTO> children;
    
    public BookChapterTreeDTO(BookChapter chapter) {
        this.id = chapter.getId();
        this.bookId = chapter.getBookId();
        this.title = chapter.getTitle();
        this.sortOrder = chapter.getSortOrder();
        this.parentId = chapter.getParentId();
        this.status = chapter.getStatus();
    }
}
