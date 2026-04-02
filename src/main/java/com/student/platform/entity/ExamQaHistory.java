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
@TableName("exam_qa_history")
public class ExamQaHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("subject")
    private String subject;

    @TableField("question_type")
    private String questionType;

    @TableField("question")
    private String question;

    @TableField("answer")
    private String answer;

    @TableField("context")
    private String context;

    @TableField("tokens_used")
    private Integer tokensUsed;

    @TableField("rating")
    private Integer rating;

    @TableField("feedback")
    private String feedback;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
