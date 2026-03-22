package com.student.platform.controller;

import com.student.platform.dto.AuthResponse;
import com.student.platform.dto.LoginRequest;
import com.student.platform.dto.RegisterRequest;
import com.student.platform.dto.Result;
import com.student.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户认证", description = "用户注册、登录等认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return Result.success("注册成功", response);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return Result.success("登录成功", response);
    }

    @Operation(summary = "验证token")
    @GetMapping("/validate")
    public Result<AuthResponse> validate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            // 返回用户信息
            AuthResponse authResponse = new AuthResponse();
            authResponse.setUsername(username);
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
            userInfo.setUsername(username);
            authResponse.setUser(userInfo);
            // 设置过期时间（24小时）
            long expiry = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
            authResponse.setExpiry(expiry);
            return Result.success("token有效", authResponse);
        }
        return Result.error(401, "token无效");
    }
}
