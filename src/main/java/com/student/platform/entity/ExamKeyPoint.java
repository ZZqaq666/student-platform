package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exam_key_point")
public class ExamKeyPoint {
    
    @TableId
    private Long id;
    private String subjectId;
    private String name;
    private String description;
    private Integer orderNum;
    private Long createTime;
    private Long updateTime;
}
