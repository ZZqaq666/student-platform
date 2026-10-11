package com.student.platform.common.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单体版 JWT 鉴权过滤器（由原网关 JwtAuthGatewayFilterFactory 移植）。
 *
 * 语义与原网关完全一致：
 * - 白名单路径直接放行
 * - 其余路径必须携带有效 Bearer JWT，否则返回 401
 * - 校验通过后向下游注入 X-User-Id / X-User-Name 请求头，
 *   供 {@link com.student.platform.user.config.UserIdInterceptor} 解析 userId
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 白名单（不含 context-path，即 /api 前缀），与原网关一致 */
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/validate",
            "/user/login",
            "/user/register",
            "/user/refresh",
            "/actuator/health",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui",
            "/swagger-ui/**",
            "/health"
    );

    private final JwtSecretProvider secretProvider;

    public JwtAuthFilter(JwtSecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (HttpMethod.OPTIONS.matches(request.getMethod())
                || isWhiteListed(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            unauthorized(response, "Missing JWT token");
            return;
        }

        String token = authHeader.substring(7);
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                    .verifyWith(secretProvider.get())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = claimsJws.getPayload();
            // 用 Number 接收，避免 jjwt 将小数字解析为 Integer 时 Long.class 强转失败
            Number userIdNum = claims.get("userId", Number.class);
            if (userIdNum == null) {
                unauthorized(response, "JWT token missing userId claim");
                return;
            }
            Long userId = userIdNum.longValue();
            String userName = claims.getSubject();

            // 同步设置 Spring Security 上下文，
            // 使依赖 SecurityContextHolder 的业务代码（如 getCurrentUser）正常工作
            if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userName, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // 注入用户身份头，供业务代码通过 UserIdInterceptor / @RequestHeader 使用
            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put("X-User-Id", String.valueOf(userId));
            if (userName != null) {
                extraHeaders.put("X-User-Name", userName);
            }
            try {
                filterChain.doFilter(new MutableHttpServletRequest(request, extraHeaders), response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            unauthorized(response, "Invalid JWT token");
        }
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private void unauthorized(HttpServletResponse response, String reason) throws IOException {
        log.warn("Rejecting request: {}", reason);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + reason + "\",\"data\":null}");
    }

    /**
     * 允许为请求追加自定义请求头的包装器
     */
    private static class MutableHttpServletRequest extends HttpServletRequestWrapper {

        private final Map<String, String> extraHeaders;

        MutableHttpServletRequest(HttpServletRequest request, Map<String, String> extraHeaders) {
            super(request);
            this.extraHeaders = extraHeaders;
        }

        @Override
        public String getHeader(String name) {
            String value = extraHeaders.get(name);
            return value != null ? value : super.getHeader(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            String value = extraHeaders.get(name);
            if (value == null) {
                return super.getHeaders(name);
            }
            return java.util.Collections.enumeration(java.util.List.of(value));
        }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            java.util.Set<String> names = new java.util.LinkedHashSet<>(extraHeaders.keySet());
            java.util.Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                names.add(original.nextElement());
            }
            return java.util.Collections.enumeration(names);
        }
    }
}
