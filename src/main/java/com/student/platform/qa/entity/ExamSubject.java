package com.student.platform.qa.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考试科目（对应 exam_subject 表，主键为字符串编码）
 */
@Data
@NoArgsConstructor
@TableName("exam_subject")
public class ExamSubject {

    private String id;

    private String name;

    private String description;

    private Integer status;
}
