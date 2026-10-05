package com.student.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToWrongBookRequest {

    @NotNull("练习题ID不能为空")
    private Long exerciseId;

    private Long answerRecordId;

    private String userAnswer;

    private String errorType;

    private String notes;

    private String tags;

    private String priority;

    private String subject;

    private String difficulty;
}
