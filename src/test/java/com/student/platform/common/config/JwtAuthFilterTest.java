package com.student.platform.common.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JwtAuthFilter 的令牌类型与吊销校验。
 *
 * 回归目标：refresh 令牌冒充 access 令牌、已登出令牌继续使用、
 * 以及 Redis 不可用时的 fail-closed 行为。
 */
class JwtAuthFilterTest {

    private static final String CONTEXT_PATH = "/api";

    private JwtSecretProvider secretProvider;
    private JwtBlacklist blacklist;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        secretProvider = new JwtSecretProvider("unit-test-secret-key-at-least-32-bytes-long-abcdefghijklmn");
        blacklist = mock(JwtBlacklist.class);
        filter = new JwtAuthFilter(secretProvider, blacklist);
    }

    private String token(String type, String jti, Long userId) {
        SecretKey key = secretProvider.get();
        return Jwts.builder()
                .subject("alice")
                .claim("userId", userId)
                .claim("type", type)
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(key)
                .compact();
    }

    private MockHttpServletRequest request(String uri, String bearer) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setContextPath(CONTEXT_PATH);
        request.setRequestURI(CONTEXT_PATH + uri.substring(CONTEXT_PATH.length()));
        if (bearer != null) {
            request.addHeader("Authorization", "Bearer " + bearer);
        }
        return request;
    }

    private MockHttpServletResponse pass(MockHttpServletRequest request) throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        // 放行时链会继续执行，且请求已被包装为携带身份的 MutableHttpServletRequest
        if (chain.getRequest() instanceof HttpServletRequest wrapped) {
            assertEquals("7", wrapped.getHeader("X-User-Id"), "放行后必须注入 X-User-Id");
        }
        return response;
    }

    @Test
    @DisplayName("合法 access 令牌放行并注入用户身份")
    void acceptsValidAccessToken() throws Exception {
        MockHttpServletResponse response = pass(request("/api/qa/history", token("access", "jti-ok", 7L)));
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("缺少 Authorization 头返回 401")
    void rejectsMissingToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/api/qa/history", null), response, new MockFilterChain());
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("refresh 令牌不得当访问凭证使用")
    void rejectsRefreshTokenAsAccess() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("/api/qa/history", token("refresh", "jti-refresh", 7L)), response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("令牌类型不正确"), response.getContentAsString());
        assertNull(chain.getRequest(), "refresh 令牌不能进入业务链");
    }

    @Test
    @DisplayName("已登出的令牌命中黑名单即拒绝")
    void rejectsRevokedToken() throws Exception {
        when(blacklist.isRevoked("jti-logout")).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("/api/qa/history", token("access", "jti-logout", 7L)), response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("令牌已失效"), response.getContentAsString());
    }

    @Test
    @DisplayName("无 jti 的令牌一律拒绝（正常签发的令牌都带 jti）")
    void rejectsWithoutTokenId() throws Exception {
        String noJti = Jwts.builder()
                .subject("alice").claim("userId", 7L).claim("type", "access")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretProvider.get())
                .compact();

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/api/qa/history", noJti), response, new MockFilterChain());
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("吊销名单查询失败时 fail-closed，不能放行")
    void failsClosedWhenBlacklistUnavailable() throws Exception {
        when(blacklist.isRevoked(anyString())).thenThrow(new IllegalStateException("Redis 连接失败"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("/api/qa/history", token("access", "jti-redis-down", 7L)), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest(), "名单不可用时必须拒绝，否则登出在故障期间形同虚设");
    }

    @Test
    @DisplayName("白名单路径无需令牌")
    void allowsWhitelistedPaths() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/api/auth/login", null), response, new MockFilterChain());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("登出接口允许携带已过期访问令牌，以便吊销刷新令牌")
    void allowsLogoutWithoutAuth() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/api/auth/logout", null), response, new MockFilterChain());
        assertEquals(200, response.getStatus());
    }
}
