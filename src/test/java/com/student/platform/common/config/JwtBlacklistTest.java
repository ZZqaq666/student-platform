package com.student.platform.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 令牌吊销名单的写入与查询语义。
 */
class JwtBlacklistTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private JwtBlacklist blacklist;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        blacklist = new JwtBlacklist(redisTemplate);
    }

    @Test
    @DisplayName("按 jti 写入并带上等于剩余有效期的 TTL")
    void revokeWritesKeyWithRemainingTtl() {
        blacklist.revoke("jti-abc", 60_000L);

        verify(valueOperations).set(eq("jwt:revoked:jti-abc"), any(), eq(Duration.ofMillis(60_000)));
    }

    @Test
    @DisplayName("已过期或无剩余有效期的令牌无需写入（JWT 自身已失效）")
    void revokeSkipsNonPositiveTtl() {
        blacklist.revoke("jti-expired", 0L);
        blacklist.revoke("jti-negative", -1L);

        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("缺失 jti 时不写入，避免产生空键")
    void revokeSkipsBlankTokenId() {
        blacklist.revoke(null, 60_000L);
        blacklist.revoke("  ", 60_000L);

        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("命中与未命中查询")
    void isRevokedReflectsKeyExistence() {
        when(redisTemplate.hasKey("jwt:revoked:bad")).thenReturn(true);
        when(redisTemplate.hasKey("jwt:revoked:good")).thenReturn(false);

        assertTrue(blacklist.isRevoked("bad"));
        assertFalse(blacklist.isRevoked("good"));
    }

    @Test
    @DisplayName("缺失 jti 视为未吊销，由调用方另行拒绝无 jti 的令牌")
    void isRevokedFalseForBlankTokenId() {
        assertFalse(blacklist.isRevoked(null));
        assertFalse(blacklist.isRevoked(" "));
        verifyNoInteractions(redisTemplate);
    }
}
