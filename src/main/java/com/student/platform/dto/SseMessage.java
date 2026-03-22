package com.student.platform.dto;

import lombok.Data;

/**
 * SSE消息模型
 */
@Data
public class SseMessage {
    /**
     * 消息类型：data - 文本内容，complete - 完成状态
     */
    private String type;
    
    /**
     * 文本内容
     */
    private String content;
    
    /**
     * 是否完成
     */
    private boolean completed;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 创建数据消息
     */
    public static SseMessage data(String content) {
        SseMessage message = new SseMessage();
        message.setType("data");
        message.setContent(content);
        message.setCompleted(false);
        return message;
    }
    
    /**
     * 创建完成消息
     */
    public static SseMessage complete() {
        SseMessage message = new SseMessage();
        message.setType("complete");
        message.setCompleted(true);
        return message;
    }
    
    /**
     * 创建错误消息
     */
    public static SseMessage error(String error) {
        SseMessage message = new SseMessage();
        message.setType("error");
        message.setError(error);
        message.setCompleted(true);
        return message;
    }
}