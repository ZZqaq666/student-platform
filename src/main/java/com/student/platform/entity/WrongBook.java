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
@TableName("wrong_book")
public class WrongBook {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("exercise_id")
    private Long exerciseId;

    @TableField("answer_record_id")
    private Long answerRecordId;

    @TableField("wrong_count")
    private Integer wrongCount;

    @TableField("correct_count")
    private Integer correctCount;

    @TableField("last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @TableField("master_status")
    private String masterStatus;

    @TableField("notes")
    private String notes;

    @TableField("tags")
    private String tags;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
