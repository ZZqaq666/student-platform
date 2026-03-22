package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("exercise")
public class Exercise {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("book_id")
    private Long bookId;

    @TableField("knowledge_node_id")
    private Long knowledgeNodeId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("question_type")
    private String questionType;

    @TableField("options")
    private String options;

    @TableField("answer")
    private String answer;

    @TableField("analysis")
    private String analysis;

    @TableField("difficulty")
    private String difficulty;

    @TableField("score")
    private Integer score;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
