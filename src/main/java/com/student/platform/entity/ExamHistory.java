package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exam_history")
public class ExamHistory {
    
    @TableId
    private Long id;
    private Long userId;
    private String subject;
    private Integer score;
    private String examDate;
    private String duration;
    private Long createTime;
}
