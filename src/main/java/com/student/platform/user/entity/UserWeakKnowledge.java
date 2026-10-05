package com.student.platform.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_weak_knowledge")
public class UserWeakKnowledge {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String knowledgeId;

    private String knowledgeName;

    private String subjectId;

    private Integer masteryLevel;

    private Integer errorCount;

    private LocalDateTime lastErrorTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
