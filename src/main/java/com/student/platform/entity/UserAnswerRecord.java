package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户答题记录实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_answer_record")
public class UserAnswerRecord {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 题目ID
     */
    private String questionId;
    
    /**
     * 题目类型
     */
    private String questionType;
    
    /**
     * 学科ID
     */
    private String subjectId;
    
    /**
     * 知识点ID
     */
    private String knowledgeId;
    
    /**
     * 答题结果（0：错误，1：正确）
     */
    private Integer result;
    
    /**
     * 答题时间（秒）
     */
    private Integer answerTime;
    
    /**
     * 难度级别
     */
    private Integer difficultyLevel;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
