package com.student.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户薄弱知识点实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_weak_knowledge")
public class UserWeakKnowledge {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
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
     * 掌握程度（0-100）
     */
    private Integer masteryLevel;
    
    /**
     * 错误次数
     */
    private Integer errorCount;
    
    /**
     * 最近错误时间
     */
    private LocalDateTime lastErrorTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
