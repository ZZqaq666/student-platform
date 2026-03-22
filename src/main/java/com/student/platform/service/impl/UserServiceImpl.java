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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (request.getEmail() != null && userMapper.findByEmail(request.getEmail()) != null) {
            throw new BusinessException("邮箱已被注册");
        }
        if (request.getPhone() != null && userMapper.findByPhone(request.getPhone()) != null) {
            throw new BusinessException("手机号已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole("STUDENT");
        user.setStatus("ACTIVE");

        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getUsername());
        // 计算token过期时间（当前时间 + 过期时间）
        long expiry = System.currentTimeMillis() + 24 * 60 * 60 * 1000; // 24小时过期

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRole(), userInfo, expiry);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        // 计算token过期时间（当前时间 + 过期时间）
        long expiry = System.currentTimeMillis() + 24 * 60 * 60 * 1000; // 24小时过期

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRole(), userInfo, expiry);
    }

    @Override
    public AuthResponse refreshToken(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        // 计算token过期时间（当前时间 + 过期时间）
        long expiry = System.currentTimeMillis() + 24 * 60 * 60 * 1000; // 24小时过期

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRole(), userInfo, expiry);
    }
}
