package com.student.platform.qa.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA 问答中心书籍（对应 book 表，轻量只读视图）
 */
@Data
@NoArgsConstructor
@TableName("book")
public class QaBook {

    private Long id;

    private String title;

    private String author;

    private String publisher;

    private String coverImage;

    private String subject;

    private String description;
}
