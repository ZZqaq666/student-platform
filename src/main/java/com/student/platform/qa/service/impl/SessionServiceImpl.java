package com.student.platform.qa.service.impl;

import com.student.platform.qa.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 会话管理服务实现
 */
@Slf4j
@Service
public class SessionServiceImpl implements SessionService {

    /**
     * 会话存储：sessionId -> 最后活跃时间戳
     */
    private final Map<String, Long> sessionStore = new ConcurrentHashMap<>();

    /**
     * 会话过期时间（秒），默认24小时
     */
    @Value("${session.expire.seconds:86400}")
    private long sessionExpireSeconds;

    /**
     * 定时清理过期会话的间隔（秒），默认5分钟
     */
    @Value("${session.cleanup.interval.seconds:300}")
    private long cleanupIntervalSeconds;

    /**
     * 安全随机数生成器
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 定时任务执行器
     */
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        // 启动定时清理过期会话的任务
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, cleanupIntervalSeconds, cleanupIntervalSeconds, TimeUnit.SECONDS);
        log.info("会话管理服务初始化完成，会话过期时间：{}秒，清理间隔：{}秒", sessionExpireSeconds, cleanupIntervalSeconds);
    }

    @Override
    public String createSession() {
        // 生成安全的会话ID：32字节随机数 + 当前时间戳的Base64编码
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String timestamp = Long.toString(Instant.now().toEpochMilli());
        String sessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes) + "-" + timestamp;
        
        // 存储会话，设置初始活跃时间
        sessionStore.put(sessionId, Instant.now().toEpochMilli());
        log.debug("创建新会话：{}", sessionId);
        return sessionId;
    }

    @Override
    public boolean validateSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        
        // 检查会话是否存在且未过期
        Long lastActivity = sessionStore.get(sessionId);
        if (lastActivity == null) {
            return false;
        }
        
        // 更新会话活跃时间
        updateSessionActivity(sessionId);
        return true;
    }

    @Override
    public boolean isSessionExpired(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return true;
        }
        
        Long lastActivity = sessionStore.get(sessionId);
        if (lastActivity == null) {
            return true;
        }
        
        long expireTime = lastActivity + sessionExpireSeconds * 1000;
        return Instant.now().toEpochMilli() > expireTime;
    }

    @Override
    public void updateSessionActivity(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            sessionStore.put(sessionId, Instant.now().toEpochMilli());
        }
    }

    @Override
    public void invalidateSession(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            sessionStore.remove(sessionId);
            log.debug("销毁会话：{}", sessionId);
        }
    }

    @Override
    public long getSessionExpireSeconds() {
        return sessionExpireSeconds;
    }

    /**
     * 清理过期会话
     */
    private void cleanupExpiredSessions() {
        long now = Instant.now().toEpochMilli();
        int removedCount = 0;
        
        for (Map.Entry<String, Long> entry : sessionStore.entrySet()) {
            long expireTime = entry.getValue() + sessionExpireSeconds * 1000;
            if (now > expireTime) {
                sessionStore.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.debug("清理过期会话：{}个", removedCount);
        }
    }
}