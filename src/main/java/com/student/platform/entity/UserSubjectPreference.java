package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户学科偏好实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_subject_preference")
public class UserSubjectPreference {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 学科ID
     */
    private String subjectId;
    
    /**
     * 学科名称
     */
    private String subjectName;
    
    /**
     * 偏好分数
     */
    private Integer preferenceScore;
    
    /**
     * 答题次数
     */
    private Integer answerCount;
    
    /**
     * 正确次数
     */
    private Integer correctCount;
}
