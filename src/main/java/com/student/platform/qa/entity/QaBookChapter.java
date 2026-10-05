package com.student.platform.qa.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA 问答中心书籍章节（对应 book_chapter 表，轻量只读视图）
 */
@Data
@NoArgsConstructor
@TableName("book_chapter")
public class QaBookChapter {

    private Long id;

    private Long bookId;

    private String title;

    private Integer sortOrder;

    private Long parentId;

    private String status;
}
