package com.student.platform.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 缓存管理器
 * 负责 AI 服务缓存的创建、获取、清理和淘汰等操作
 */
@Slf4j
@Component
public class AiCacheManager {

    @Value("${ai.cache.max-size:1000}")
    private int maxCacheSize;

    @Value("${ai.cache.expiry-hours:24}")
    private int cacheExpiryHours;

    private final Map<String, CacheItem> cache = new ConcurrentHashMap<>();

    /**
     * 缓存项
     */
    private class CacheItem {
        private final String content;
        private final Instant createdAt;

        public CacheItem(String content) {
            this.content = content;
            this.createdAt = Instant.now();
        }

        public String getContent() {
            return content;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(Duration.ofHours(cacheExpiryHours)));
        }

        public Instant getCreatedAt() {
            return createdAt;
        }
    }

    /**
     * 从缓存中获取内容
     * @param key 缓存键
     * @return 缓存的内容，如果缓存不存在或已过期则返回null
     */
    public String get(String key) {
        CacheItem item = cache.get(key);
        if (item != null) {
            if (item.isExpired()) {
                cache.remove(key);
                log.info("缓存项已过期并移除: {}", key);
                return null;
            }
            log.info("从缓存中获取响应: {}", key);
            return item.getContent();
        }
        return null;
    }

    /**
     * 添加内容到缓存
     * @param key 缓存键
     * @param content 缓存内容
     */
    public void put(String key, String content) {
        // 清理过期缓存
        cleanupExpiredCache();

        // 检查缓存大小，达到上限则移除最旧的缓存项
        if (cache.size() >= maxCacheSize) {
            evictOldestCacheItem();
        }

        cache.put(key, new CacheItem(content));
        log.info("缓存响应: {}, 当前缓存大小: {}", key, cache.size());
    }

    /**
     * 清理过期的缓存项
     */
    public void cleanupExpiredCache() {
        List<String> expiredKeys = new ArrayList<>();
        for (Map.Entry<String, CacheItem> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredKeys.add(entry.getKey());
            }
        }

        for (String key : expiredKeys) {
            cache.remove(key);
        }

        if (!expiredKeys.isEmpty()) {
            log.info("清理过期缓存项: {} 个", expiredKeys.size());
        }
    }

    /**
     * 移除最旧的缓存项
     */
    public void evictOldestCacheItem() {
        String oldestKey = null;
        Instant oldestTime = Instant.now();

        for (Map.Entry<String, CacheItem> entry : cache.entrySet()) {
            Instant createdAt = entry.getValue().getCreatedAt();
            if (createdAt.isBefore(oldestTime)) {
                oldestTime = createdAt;
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
            log.info("缓存达到上限，移除最旧的缓存项: {}", oldestKey);
        }
    }

    /**
     * 获取当前缓存大小
     * @return 缓存大小
     */
    public int size() {
        return cache.size();
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
        log.info("缓存已清空");
    }
}
