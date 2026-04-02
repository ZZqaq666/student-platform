package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 真题试卷实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exam_paper")
public class ExamPaper {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 试卷唯一标识
     */
    private String paperId;
    
    /**
     * 年份
     */
    private Integer year;
    
    /**
     * 考试类型
     */
    private String examType;
    
    /**
     * 学科ID
     */
    private String subjectId;
    
    /**
     * 学科名称
     */
    private String subjectName;
    
    /**
     * 试卷名称
     */
    private String paperName;
    
    /**
     * 总题数
     */
    private Integer totalQuestions;
    
    /**
     * 总分数
     */
    private Integer totalScore;
    
    /**
     * 难度系数
     */
    private Double difficulty;
    
    /**
     * 来源
     */
    private String source;
    
    /**
     * 解析状态
     */
    private Integer analysisStatus;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
