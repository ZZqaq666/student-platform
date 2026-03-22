package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exam_subject")
public class ExamSubject {
    
    @TableId
    private String id;
    private String name;
    private String description;
    private Integer status;
    private Long createTime;
    private Long updateTime;
}
