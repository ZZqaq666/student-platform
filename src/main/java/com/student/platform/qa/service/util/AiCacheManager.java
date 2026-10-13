package com.student.platform.qa.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 回答缓存。
 *
 * 此前是进程内 ConcurrentHashMap（"分布式缓存"名不副实）：多实例各存一套、
 * 重启即失、且每次 put 都要全表扫描清理过期项并按时间戳挑一个淘汰（O(n)）。
 * 换成 Redis 后过期交给 TTL，不再有扫描与淘汰逻辑。
 *
 * 键必须由调用方保证含用户维度（见 AiServiceImpl#buildCacheKey），否则不同用户
 * 的问答会互相命中——那正是原来串台的成因。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCacheManager {

    private static final String KEY_PREFIX = "ai:answer:";

    @Value("${ai.cache.expiry-hours:24}")
    private int cacheExpiryHours;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * key 为 null 表示"本次调用没有用户归属，不缓存"，读写一并跳过。
     */
    public String get(String key) {
        if (key == null) {
            return null;
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + key);
            if (value != null) {
                log.debug("命中 AI 回答缓存: {}", key);
            }
            return value;
        } catch (Exception e) {
            // 缓存不是正确性依赖：Redis 抖动时退化为"没有缓存"，不能让问答失败
            log.warn("读取 AI 回答缓存失败，按未命中处理: {}", e.getMessage());
            return null;
        }
    }

    public void put(String key, String content) {
        if (key == null || content == null || content.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    KEY_PREFIX + key, content, Duration.ofHours(Math.max(1, cacheExpiryHours)));
        } catch (Exception e) {
            log.warn("写入 AI 回答缓存失败，忽略: {}", e.getMessage());
        }
    }
}
