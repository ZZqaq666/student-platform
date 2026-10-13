package com.student.platform.user.service.impl;

import com.student.platform.common.config.JwtBlacklist;
import com.student.platform.common.dto.AuthResponse;
import com.student.platform.common.dto.LoginRequest;
import com.student.platform.common.dto.RegisterRequest;
import com.student.platform.common.exception.BusinessException;
import com.student.platform.common.util.JwtUtil;
import com.student.platform.common.entity.User;
import com.student.platform.user.mapper.UserMapper;
import com.student.platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtBlacklist jwtBlacklist;

    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{4,20}$";
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{6,20}$";
    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        log.info("用户注册请求, username: {}", request.getUsername());

        validateRegisterRequest(request);

        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException(409, "用户名已存在");
        }

        if (StringUtils.hasText(request.getEmail()) && userMapper.findByEmail(request.getEmail()) != null) {
            throw new BusinessException(409, "邮箱已被注册");
        }

        if (StringUtils.hasText(request.getPhone()) && userMapper.findByPhone(request.getPhone()) != null) {
            throw new BusinessException(409, "手机号已被注册");
        }

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

        userMapper.insert(user);
        log.info("用户注册成功, userId: {}, username: {}", user.getId(), user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());
        long expiry = System.currentTimeMillis() + getExpirationTime();

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
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE_BEARER)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .user(userInfo)
                .expiry(expiry)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("用户登录请求, username: {}", request.getUsername());

        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }

        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            log.warn("登录失败，用户不存在: {}", request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (STATUS_DISABLED.equals(user.getStatus())) {
            log.warn("登录失败，账号已被禁用: {}", request.getUsername());
            throw new BusinessException(403, "账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败，密码错误: {}", request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());
        long expiry = System.currentTimeMillis() + getExpirationTime();

        log.info("用户登录成功, userId: {}, username: {}", user.getId(), user.getUsername());

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
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE_BEARER)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .user(userInfo)
                .expiry(expiry)
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("刷新令牌请求");

        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(400, "刷新令牌不能为空");
        }

        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(401, "刷新令牌无效或已过期");
        }

        // /auth/refresh 在 JwtAuthFilter 白名单里，过滤器不会替我们查吊销名单，
        // 所以这里必须自己查——否则登出与被轮换掉的刷新令牌依然能续期。
        // Redis 不可用时按已吊销处理（fail-closed），与 JwtAuthFilter 保持一致。
        String currentTokenId = jwtUtil.extractTokenId(refreshToken);
        if (isTokenRevoked(currentTokenId)) {
            throw new BusinessException(401, "刷新令牌已失效，请重新登录");
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

        String newToken = jwtUtil.generateToken(username, userId, user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(username, userId);
        long expiry = System.currentTimeMillis() + getExpirationTime();

        // 轮换：旧刷新令牌一次性使用，换出新令牌后立即作废，
        // 被复制走的旧令牌再用来续期就会命中吊销名单。
        jwtBlacklist.revoke(currentTokenId, jwtUtil.getExpirationTime(refreshToken));

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
                .refreshToken(newRefreshToken)
                .tokenType(TOKEN_TYPE_BEARER)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .user(userInfo)
                .expiry(expiry)
                .build();
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!request.getUsername().matches(USERNAME_PATTERN)) {
            throw new BusinessException(400, "用户名格式错误，应为4-20位字母、数字或下划线");
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }
        if (!request.getPassword().matches(PASSWORD_PATTERN)) {
            throw new BusinessException(400, "密码格式错误，应为6-20位，至少包含字母和数字");
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().matches(EMAIL_PATTERN)) {
            throw new BusinessException(400, "邮箱格式错误");
        }

        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().matches(PHONE_PATTERN)) {
            throw new BusinessException(400, "手机号格式错误");
        }
    }

    /**
     * 吊销名单查询。Redis 不可用时按已吊销处理（fail-closed），
     * 与 JwtAuthFilter 的策略保持一致，否则登出在故障期间形同虚设。
     */
    private boolean isTokenRevoked(String tokenId) {
        try {
            return jwtBlacklist.isRevoked(tokenId);
        } catch (Exception e) {
            log.error("查询令牌吊销名单失败，按已吊销处理: {}", e.getMessage());
            return true;
        }
    }

    private long getExpirationTime() {
        // 与令牌真实 exp 同源。原先硬编码 24 小时，而 jwt.expiration 已改为
        // 15 分钟，导致返回给前端的 expiry 比令牌实际寿命多出 96 倍。
        return jwtUtil.getAccessTokenTtlMillis();
    }
}
