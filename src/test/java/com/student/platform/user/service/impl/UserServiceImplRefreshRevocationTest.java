package com.student.platform.user.service.impl;

import com.student.platform.common.config.JwtBlacklist;
import com.student.platform.common.config.JwtSecretProvider;
import com.student.platform.common.dto.AuthResponse;
import com.student.platform.common.entity.User;
import com.student.platform.common.exception.BusinessException;
import com.student.platform.common.util.JwtUtil;
import com.student.platform.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 刷新链路的吊销校验。
 *
 * 回归背景：/auth/refresh 在 JwtAuthFilter 白名单里，过滤器不查吊销名单，
 * 而 refreshToken() 原先只校验签名与 type——导致登出后的刷新令牌、以及被
 * 轮换掉的旧刷新令牌仍然能换出新 access。这个洞是跑真实 HTTP 才暴露的。
 */
class UserServiceImplRefreshRevocationTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-bytes-long-abcdefghijklmn";

    private JwtUtil jwtUtil;
    private JwtBlacklist blacklist;
    private UserMapper userMapper;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(new JwtSecretProvider(SECRET));
        ReflectionTestUtils.setField(jwtUtil, "expiration", 900_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604_800_000L);

        blacklist = mock(JwtBlacklist.class);
        userMapper = mock(UserMapper.class);
        service = new UserServiceImpl(userMapper, mock(PasswordEncoder.class), jwtUtil, blacklist);

        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setRole("STUDENT");
        user.setStatus("ACTIVE");
        when(userMapper.findByUsername("alice")).thenReturn(user);
    }

    @Test
    @DisplayName("已登出的刷新令牌不能再换出访问令牌")
    void rejectsRevokedRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken("alice", 7L);
        String tokenId = jwtUtil.extractTokenId(refreshToken);
        when(blacklist.isRevoked(tokenId)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.refreshToken(refreshToken));
        assertEquals(401, ex.getCode());
        verify(blacklist, never()).revoke(anyString(), anyLong());
    }

    @Test
    @DisplayName("有效刷新令牌换出新对，并立刻作废旧的")
    void rotatesAndRevokesOldRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken("alice", 7L);
        String oldTokenId = jwtUtil.extractTokenId(refreshToken);

        AuthResponse response = service.refreshToken(refreshToken);

        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken(), "必须回轮换来的新刷新令牌");
        verify(blacklist).revoke(eq(oldTokenId), anyLong());
    }

    @Test
    @DisplayName("名单查询失败按已吊销处理（fail-closed）")
    void failsClosedWhenBlacklistUnavailable() {
        String refreshToken = jwtUtil.generateRefreshToken("alice", 7L);
        when(blacklist.isRevoked(anyString())).thenThrow(new IllegalStateException("Redis 连接失败"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.refreshToken(refreshToken));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("access 令牌不能冒充 refresh 令牌")
    void rejectsAccessTokenAsRefresh() {
        String accessToken = jwtUtil.generateToken("alice", 7L, "STUDENT");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.refreshToken(accessToken));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("返回给前端的 expiry 与令牌真实寿命同源")
    void reportedExpiryMatchesConfiguredAccessTokenTtl() {
        String refreshToken = jwtUtil.generateRefreshToken("alice", 7L);

        AuthResponse response = service.refreshToken(refreshToken);

        long ttl = response.getExpiry() - System.currentTimeMillis();
        // 允许秒级误差：真实令牌 exp 应为 15 分钟，而不是过去硬编码的 24 小时
        assertTrue(Math.abs(ttl - 900_000L) < 15_000L,
                "expiry 应接近 15 分钟，实际=" + (ttl / 1000) + "s");
    }
}
