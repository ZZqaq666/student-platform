package com.student.platform.user.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserIdInterceptor implements HandlerInterceptor {
    public static final String USER_ID_ATTRIBUTE = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 安全基线：userId 只认 JwtAuthFilter 注入的 X-User-Id 请求头（该头由过滤器注入且优先级最高）。
        // 绝不能从 URL 参数兜底取值，否则攻击者可用 ?userId=xxx 直接冒用他人身份。
        String userIdStr = request.getHeader("X-User-Id");

        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdStr);
                request.setAttribute(USER_ID_ATTRIBUTE, userId);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Invalid userId format");
                return false;
            }
        }

        return true;
    }
}
