package com.student.platform.exercise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_answer_record")
public class UserAnswerRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String questionId;

    private String questionType;

    private String subjectId;

    private String knowledgeId;

    private Integer result;

    private Integer answerTime;

    private Integer difficultyLevel;

    private LocalDateTime createdAt;
}
