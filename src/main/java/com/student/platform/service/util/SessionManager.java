package com.student.platform.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器
 * 负责会话的创建、获取、清理和淘汰等操作
 */
@Slf4j
@Component
public class SessionManager {

    @Value("${ai.session.max-sessions:1000}")
    private int maxSessions;

    @Value("${ai.session.expiry-hours:2}")
    private int sessionExpiryHours;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * 会话类
     */
    public class Session {
        private final String sessionId;
        private final List<String> messages = new ArrayList<>();
        private final Instant createdAt;

        public Session(String sessionId) {
            this.sessionId = sessionId;
            this.createdAt = Instant.now();
        }

        public String getSessionId() {
            return sessionId;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void addMessage(String message) {
            messages.add(message);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(Duration.ofHours(sessionExpiryHours)));
        }

        public Instant getCreatedAt() {
            return createdAt;
        }
    }

    /**
     * 创建新会话
     * @return 会话ID
     */
    public String createSession() {
        // 清理过期会话
        cleanupExpiredSessions();

        // 检查会话数量，达到上限则移除最旧的会话
        if (sessions.size() >= maxSessions) {
            evictOldestSession();
        }

        String sessionId = generateSessionId();
        sessions.put(sessionId, new Session(sessionId));
        log.info("创建新会话: {}, 当前会话数: {}", sessionId, sessions.size());
        return sessionId;
    }

    /**
     * 获取会话
     * @param sessionId 会话ID
     * @return 会话对象，如果会话不存在或已过期则返回null
     */
    public Session getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }

        Session session = sessions.get(sessionId);
        if (session != null) {
            if (session.isExpired()) {
                sessions.remove(sessionId);
                log.info("会话已过期并移除: {}", sessionId);
                return null;
            }
            return session;
        }
        return null;
    }

    /**
     * 添加消息到会话
     * @param sessionId 会话ID
     * @param message 消息内容
     */
    public void addMessage(String sessionId, String message) {
        Session session = getSession(sessionId);
        if (session != null) {
            session.addMessage(message);
            log.info("更新会话消息，会话ID：{}, 当前消息数：{}", sessionId, session.getMessages().size());
        }
    }

    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions() {
        List<String> expiredSessionIds = new ArrayList<>();
        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredSessionIds.add(entry.getKey());
            }
        }

        for (String sessionId : expiredSessionIds) {
            sessions.remove(sessionId);
        }

        if (!expiredSessionIds.isEmpty()) {
            log.info("清理过期会话: {} 个", expiredSessionIds.size());
        }
    }

    /**
     * 移除最旧的会话
     */
    public void evictOldestSession() {
        String oldestSessionId = null;
        Instant oldestTime = Instant.now();

        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            Instant createdAt = entry.getValue().getCreatedAt();
            if (createdAt.isBefore(oldestTime)) {
                oldestTime = createdAt;
                oldestSessionId = entry.getKey();
            }
        }

        if (oldestSessionId != null) {
            sessions.remove(oldestSessionId);
            log.info("会话达到上限，移除最旧的会话: {}", oldestSessionId);
        }
    }

    /**
     * 获取当前会话数量
     * @return 会话数量
     */
    public int size() {
        return sessions.size();
    }

    /**
     * 生成会话ID
     * @return 会话ID
     */
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}