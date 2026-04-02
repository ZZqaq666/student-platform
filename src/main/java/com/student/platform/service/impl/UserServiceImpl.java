package com.student.platform.service.impl;

import com.student.platform.dto.AuthResponse;
import com.student.platform.dto.LoginRequest;
import com.student.platform.dto.RegisterRequest;
import com.student.platform.entity.User;
import com.student.platform.exception.BusinessException;
import com.student.platform.mapper.UserMapper;
import com.student.platform.service.UserService;
import com.student.platform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 * <p>
 * 提供用户注册、登录、认证等核心功能
 * 包含密码加密、JWT令牌生成等安全机制
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    // 用户名正则：4-20位字母、数字、下划线
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{4,20}$";
    // 密码正则：6-20位，至少包含字母和数字
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{6,20}$";
    // 手机号正则
    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";
    // 邮箱正则
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证响应，包含JWT令牌
     * @throws BusinessException 如果用户名已存在或参数无效
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        log.info("用户注册请求, username: {}", request.getUsername());

        // 参数校验
        validateRegisterRequest(request);

        // 检查用户名是否已存在
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException(409, "用户名已存在");
        }

        // 检查邮箱是否已被注册
        if (StringUtils.hasText(request.getEmail()) && userMapper.findByEmail(request.getEmail()) != null) {
            throw new BusinessException(409, "邮箱已被注册");
        }

        // 检查手机号是否已被注册
        if (StringUtils.hasText(request.getPhone()) && userMapper.findByPhone(request.getPhone()) != null) {
            throw new BusinessException(409, "手机号已被注册");
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setRole(ROLE_STUDENT);
        user.setStatus(STATUS_ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 保存用户
        userMapper.insert(user);
        log.info("用户注册成功, userId: {}, username: {}", user.getId(), user.getUsername());

        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());
        long expiry = System.currentTimeMillis() + getExpirationTime();

        // 构建响应
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone()
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType(TOKEN_TYPE_BEARER)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .user(userInfo)
                .expiry(expiry)
                .build();
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证响应，包含JWT令牌
     * @throws BusinessException 如果用户名或密码错误，或账号被禁用
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("用户登录请求, username: {}", request.getUsername());

        // 参数校验
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }

        // 查询用户
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            log.warn("登录失败，用户不存在: {}", request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 检查账号状态
        if (STATUS_DISABLED.equals(user.getStatus())) {
            log.warn("登录失败，账号已被禁用: {}", request.getUsername());
            throw new BusinessException(403, "账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败，密码错误: {}", request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 更新最后登录时间(使用updated_at字段)
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());
        long expiry = System.currentTimeMillis() + getExpirationTime();

        log.info("用户登录成功, userId: {}, username: {}", user.getId(), user.getUsername());

        // 构建响应
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone()
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType(TOKEN_TYPE_BEARER)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .user(userInfo)
                .expiry(expiry)
                .build();
    }

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的认证响应
     * @throws BusinessException 如果刷新令牌无效
     */
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("刷新令牌请求");

        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(400, "刷新令牌不能为空");
        }

        // 验证刷新令牌
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(401, "刷新令牌无效或已过期");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        Long userId = jwtUtil.extractUserId(refreshToken);

        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 生成新的访问令牌
        String newToken = jwtUtil.generateToken(username, userId, user.getRole());
        long expiry = System.currentTimeMillis() + getExpirationTime();

        log.info("令牌刷新成功, userId: {}, username: {}", user.getId(), user.getUsername());

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone()
        );

        return AuthResponse.builder()
                .token(newToken)
                .tokenType(TOKEN_TYPE_BEARER)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .user(userInfo)
                .expiry(expiry)
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证注册请求参数
     *
     * @param request 注册请求
     * @throws BusinessException 如果参数无效
     */
    private void validateRegisterRequest(RegisterRequest request) {
        // 用户名验证
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!request.getUsername().matches(USERNAME_PATTERN)) {
            throw new BusinessException(400, "用户名格式错误，应为4-20位字母、数字或下划线");
        }

        // 密码验证
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }
        if (!request.getPassword().matches(PASSWORD_PATTERN)) {
            throw new BusinessException(400, "密码格式错误，应为6-20位，至少包含字母和数字");
        }

        // 邮箱验证(如果提供)
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().matches(EMAIL_PATTERN)) {
            throw new BusinessException(400, "邮箱格式错误");
        }

        // 手机号验证(如果提供)
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().matches(PHONE_PATTERN)) {
            throw new BusinessException(400, "手机号格式错误");
        }
    }

    /**
     * 获取令牌过期时间
     *
     * @return 过期时间(毫秒)
     */
    private long getExpirationTime() {
        // 默认24小时
        return 24 * 60 * 60 * 1000;
    }
}
