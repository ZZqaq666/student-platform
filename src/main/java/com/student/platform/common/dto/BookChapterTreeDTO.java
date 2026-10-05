package com.student.platform.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BookChapterTreeDTO {

    private Long id;
    private Long bookId;
    private String title;
    private Integer sortOrder;
    private String content;
    private Long parentId;
    private String status;
    private List<BookChapterTreeDTO> children;

}
