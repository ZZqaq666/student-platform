package com.student.platform.qa.service;

import com.student.platform.qa.entity.Message;

import java.util.List;

/**
 * 消息管理服务接口
 */
public interface MessageService {

    /**
     * 保存消息
     * @param sessionId 会话ID
     * @param messageId 消息ID
     * @param role 角色（user/assistant/system）
     * @param content 消息内容
     * @param tokensUsed 消耗的token数量
     * @param status 状态
     * @return 保存的消息
     */
    Message saveMessage(String sessionId, String messageId, String role, String content, 
                        Integer tokensUsed, String status);

    /**
     * 根据会话ID获取所有消息
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<Message> getMessagesBySessionId(String sessionId);

    /**
     * 根据会话ID分页获取消息
     * @param sessionId 会话ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 消息列表
     */
    List<Message> getMessagesBySessionIdWithPagination(String sessionId, int page, int size);

    /**
     * 获取会话的消息数量
     * @param sessionId 会话ID
     * @return 消息数量
     */
    int getMessageCount(String sessionId);

    /**
     * 更新消息状态
     * @param messageId 消息ID
     * @param status 新状态
     */
    void updateMessageStatus(String messageId, String status);

    /**
     * 更新消息内容
     * @param messageId 消息ID
     * @param content 新内容
     */
    void updateMessageContent(String messageId, String content);

    /**
     * 删除会话的所有消息
     * @param sessionId 会话ID
     */
    void deleteMessagesBySessionId(String sessionId);
}