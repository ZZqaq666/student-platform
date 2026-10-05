package com.student.platform.qa.service;

import com.student.platform.qa.entity.Conversation;
import com.student.platform.qa.entity.Message;

import java.util.List;

/**
 * 会话管理服务接口
 */
public interface ConversationService {

    /**
     * 创建会话
     * @param userId 用户ID
     * @param firstMessage 第一条消息内容（用于生成标题）
     * @return 创建的会话
     */
    Conversation createConversation(Long userId, String firstMessage);

    /**
     * 根据sessionId获取会话
     * @param sessionId 会话ID
     * @return 会话信息
     */
    Conversation getConversationBySessionId(String sessionId);

    /**
     * 根据sessionId和userId获取会话（权限验证）
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 会话信息
     */
    Conversation getConversationBySessionIdAndUserId(String sessionId, Long userId);

    /**
     * 获取用户的所有活跃会话
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Conversation> getUserConversations(Long userId);

    /**
     * 更新会话（最后活跃时间、标题、最后消息预览）
     * @param sessionId 会话ID
     * @param lastMessage 最后一条消息
     */
    void updateConversation(String sessionId, String lastMessage);

    /**
     * 删除会话（逻辑删除）
     * @param sessionId 会话ID
     * @param userId 用户ID（用于权限验证）
     */
    void deleteConversation(String sessionId, Long userId);

    /**
     * 检查会话是否属于用户
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 是否属于该用户
     */
    boolean isConversationOwner(String sessionId, Long userId);

    /**
     * 获取会话的消息数量
     * @param sessionId 会话ID
     * @return 消息数量
     */
    int getMessageCount(String sessionId);
}