package com.student.platform.qa.service;

import java.util.List;

/**
 * 上下文处理服务接口 - 管理对话上下文和历史记录
 */
public interface ContextService {

    /**
     * 获取处理后的上下文（用于输入给大模型）
     * @param sessionId 会话ID
     * @param currentQuestion 当前问题
     * @return 拼接好的上下文字符串
     */
    String getContext(String sessionId, String currentQuestion);

    /**
     * 保存对话消息到上下文
     * @param sessionId 会话ID
     * @param messageId 消息ID
     * @param role 角色（user/assistant）
     * @param content 消息内容
     */
    void saveMessage(String sessionId, String messageId, String role, String content);

    /**
     * 获取会话的所有历史消息
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ContextMessage> getHistoryMessages(String sessionId);

    /**
     * 清空会话上下文
     * @param sessionId 会话ID
     */
    void clearContext(String sessionId);

    /**
     * 获取当前上下文的token数量
     * @param sessionId 会话ID
     * @return token数量
     */
    int getContextTokenCount(String sessionId);

    /**
     * 上下文消息记录
     */
    record ContextMessage(
        String messageId,
        String role,
        String content,
        long timestamp
    ) {}
}