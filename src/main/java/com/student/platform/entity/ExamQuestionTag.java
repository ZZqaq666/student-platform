package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 真题题目标签实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exam_question_tag")
public class ExamQuestionTag {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 题目ID
     */
    private String questionId;
    
    /**
     * 标签名称
     */
    private String tagName;
    
    /**
     * 标签类型
     */
    private String tagType;
}
