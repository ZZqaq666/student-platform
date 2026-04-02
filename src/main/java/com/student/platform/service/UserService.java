package com.student.platform.service;

import com.student.platform.dto.AuthResponse;
import com.student.platform.dto.LoginRequest;
import com.student.platform.dto.RegisterRequest;

/**
 * 用户服务接口
 * <p>
 * 提供用户注册、登录、令牌刷新等核心功能
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证响应，包含JWT令牌
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证响应，包含JWT令牌
     */
    AuthResponse login(LoginRequest request);

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的认证响应
     */
    AuthResponse refreshToken(String refreshToken);
}
