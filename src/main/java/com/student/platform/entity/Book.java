package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("author")
    private String author;

    @TableField("publisher")
    private String publisher;

    @TableField("isbn")
    private String isbn;

    @TableField("cover_image")
    private String coverImage;

    @TableField("description")
    private String description;

    @TableField("subject")
    private String subject;

    @TableField("grade")
    private String grade;

    @TableField("version")
    private String version;

    @TableField("status")
    private String status;

    @TableField("category")
    private String category;

    @TableField("course_type")
    private String courseType;

    @TableField("major")
    private String major;

    @TableField("semester")
    private String semester;

    @TableField("university_level")
    private String universityLevel;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
