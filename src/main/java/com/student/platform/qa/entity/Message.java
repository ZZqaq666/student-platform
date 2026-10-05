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
 * 消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 消息ID（外部使用的唯一标识）
     */
    private String messageId;

    /**
     * 发送者角色：user-用户，assistant-助手，system-系统
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消耗的token数量
     */
    private Integer tokensUsed;

    /**
     * 状态：SENDING-发送中，SUCCESS-成功，FAILED-失败
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