package com.student.platform.common.config;

import com.student.platform.common.exception.BusinessException;
import com.student.platform.user.config.UserIdInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * 当前登录用户提供者。
 *
 * 背景：答疑专区原先把作者硬编码为 'u-current' / '我'，导致提问人身份丢失、
 * 「采纳回答」按钮永远不可用。本类统一从请求上下文解析真实用户。
 *
 * 解析顺序：
 * 1. UserIdInterceptor 写入的 request 属性 userId（来自网关/过滤器注入的 X-User-Id）
 * 2. 请求头 X-User-Id
 * 3. SecurityContext 中的 username（JWT subject）
 *
 * 注意：@Async 线程中会丢失 RequestContext 与 SecurityContext，
 * 因此异步任务必须提前把用户信息作为参数传入，不能在异步线程内调用本类。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取当前登录用户，未登录时抛 401。
     */
    public CurrentUser require() {
        CurrentUser currentUser = find();
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户，未登录时返回 null（用于「是否本人」这类非强制判断）。
     */
    public CurrentUser find() {
        Long userId = resolveUserId();
        if (userId != null) {
            return loadById(userId);
        }

        String username = resolveUsername();
        if (username != null) {
            return loadByUsername(username);
        }

        return null;
    }

    private Long resolveUserId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object value = attributes.getRequest().getAttribute(UserIdInterceptor.USER_ID_ATTRIBUTE);
            Long parsed = toLong(value);
            if (parsed != null) {
                return parsed;
            }
            parsed = toLong(attributes.getRequest().getHeader("X-User-Id"));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }
        return username;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.warn("无法解析用户ID：{}", value);
            return null;
        }
    }

    private CurrentUser loadById(Long userId) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT id, username, COALESCE(NULLIF(TRIM(nickname), ''), username) AS display_name,"
                            + " status FROM user WHERE id = ?",
                    userId
            );
            return toCurrentUser(row);
        } catch (Exception e) {
            log.warn("按ID查询用户失败，userId={}：{}", userId, e.getMessage());
            return null;
        }
    }

    private CurrentUser loadByUsername(String username) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT id, username, COALESCE(NULLIF(TRIM(nickname), ''), username) AS display_name,"
                            + " status FROM user WHERE username = ?",
                    username
            );
            return toCurrentUser(row);
        } catch (Exception e) {
            log.warn("按用户名查询用户失败，username={}：{}", username, e.getMessage());
            return null;
        }
    }

    private CurrentUser toCurrentUser(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        Object status = row.get("status");
        if (status != null && "DISABLED".equalsIgnoreCase(String.valueOf(status))) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return new CurrentUser(
                ((Number) row.get("id")).longValue(),
                String.valueOf(row.get("username")),
                String.valueOf(row.get("display_name"))
        );
    }

    /**
     * 当前登录用户快照。
     */
    public record CurrentUser(Long id, String username, String displayName) {
    }
}
