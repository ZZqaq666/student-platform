package com.student.platform.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 吊销名单。
 *
 * 登出或被轮换的令牌按 jti 记入 Redis，TTL 取令牌自身的剩余有效期，
 * 到期自动消失，因此无需清理任务，名单也不会无限增长。
 *
 * Redis 不可用时 {@link #isRevoked} 会把底层异常抛给调用方，由 JwtAuthFilter
 * 统一按 401 处理（fail-closed）。这里刻意不做「查不到就当没吊销」的降级：
 * 一旦降级，登出与轮换在故障期间形同虚设，而这正是本类要防的场景。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtBlacklist {

    private static final String KEY_PREFIX = "jwt:revoked:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void revoke(String tokenId, long remainingMillis) {
        if (tokenId == null || tokenId.isBlank() || remainingMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, 1, Duration.ofMillis(remainingMillis));
        log.debug("令牌已吊销, jti={}, 剩余有效期={}ms", tokenId, remainingMillis);
    }

    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
    }
}
