package com.student.platform.qa.service;

/**
 * 会话管理服务接口
 */
public interface SessionService {

    /**
     * 生成新的会话ID
     * @return 新生成的会话ID
     */
    String createSession();

    /**
     * 验证会话ID是否有效
     * @param sessionId 会话ID
     * @return 是否有效
     */
    boolean validateSession(String sessionId);

    /**
     * 检查会话ID是否过期
     * @param sessionId 会话ID
     * @return 是否过期
     */
    boolean isSessionExpired(String sessionId);

    /**
     * 更新会话最后活跃时间
     * @param sessionId 会话ID
     */
    void updateSessionActivity(String sessionId);

    /**
     * 销毁会话
     * @param sessionId 会话ID
     */
    void invalidateSession(String sessionId);

    /**
     * 获取会话过期时间（秒）
     * @return 过期时间
     */
    long getSessionExpireSeconds();
}