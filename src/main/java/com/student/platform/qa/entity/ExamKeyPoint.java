package com.student.platform.qa.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考试考点（对应 exam_key_point 表）
 */
@Data
@NoArgsConstructor
@TableName("exam_key_point")
public class ExamKeyPoint {

    private Long id;

    private String subjectId;

    private String name;

    private String description;

    private Integer orderNum;
}
