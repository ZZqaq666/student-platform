package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 真题题目实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exam_question")
public class ExamQuestion {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 题目唯一标识
     */
    private String questionId;
    
    /**
     * 试卷ID
     */
    private String paperId;
    
    /**
     * 题目类型
     */
    private String questionType;
    
    /**
     * 题目内容
     */
    private String content;
    
    /**
     * 选项（JSON格式）
     */
    private String options;
    
    /**
     * 正确答案
     */
    private String correctAnswer;
    
    /**
     * 解析
     */
    private String analysis;
    
    /**
     * 分数
     */
    private Integer score;
    
    /**
     * 难度级别
     */
    private Integer difficultyLevel;
    
    /**
     * 知识点ID
     */
    private String knowledgeId;
    
    /**
     * 知识点名称
     */
    private String knowledgeName;
    
    /**
     * 学科ID
     */
    private String subjectId;
    
    /**
     * 学科名称
     */
    private String subjectName;
    
    /**
     * 年份
     */
    private Integer year;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
