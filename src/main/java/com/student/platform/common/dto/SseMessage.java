package com.student.platform.common.dto;

import lombok.Data;

@Data
public class SseMessage {
    private String type;
    private String content;
    private boolean completed;
    private String error;
    private String sessionId;

    public static SseMessage data(String content) {
        SseMessage message = new SseMessage();
        message.setType("data");
        message.setContent(content);
        message.setCompleted(false);
        return message;
    }

    public static SseMessage data(String content, String sessionId) {
        SseMessage message = new SseMessage();
        message.setType("data");
        message.setContent(content);
        message.setCompleted(false);
        message.setSessionId(sessionId);
        return message;
    }

    public static SseMessage complete() {
        SseMessage message = new SseMessage();
        message.setType("complete");
        message.setCompleted(true);
        return message;
    }

    public static SseMessage complete(String sessionId) {
        SseMessage message = new SseMessage();
        message.setType("complete");
        message.setCompleted(true);
        message.setSessionId(sessionId);
        return message;
    }

    public static SseMessage error(String error) {
        SseMessage message = new SseMessage();
        message.setType("error");
        message.setError(error);
        message.setCompleted(true);
        return message;
    }

    public boolean isData() {
        return "data".equals(this.type);
    }

    public String getData() {
        return this.content;
    }
}
