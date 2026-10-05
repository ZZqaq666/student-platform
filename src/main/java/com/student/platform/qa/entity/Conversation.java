package com.student.platform.qa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID（外部使用的唯一标识）
     */
    private String sessionId;

    /**
     * 会话标题（取第一条消息前20个字符）
     */
    private String title;

    /**
     * 最后一条消息预览
     */
    private String lastMessagePreview;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 状态：ACTIVE-活跃，DELETED-已删除
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}