package com.student.platform.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习笔记实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("note")
public class Note {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 */
    @TableField("user_id")
    private Long userId;

    /** 教材ID */
    @TableField("book_id")
    private Long bookId;

    /** 关联章节（可选） */
    @TableField("chapter_id")
    private Long chapterId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
