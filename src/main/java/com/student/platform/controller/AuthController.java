package com.student.platform.controller;

import com.student.platform.dto.AuthResponse;
import com.student.platform.dto.LoginRequest;
import com.student.platform.dto.RefreshTokenRequest;
import com.student.platform.dto.RegisterRequest;
import com.student.platform.dto.Result;
import com.student.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 * <p>
 * 提供用户注册、登录、令牌验证等认证相关接口
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
@Tag(name = "用户认证", description = "用户注册、登录、令牌刷新等认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证响应
     */
    @Operation(
            summary = "用户注册",
            description = "注册新用户账号，用户名4-20位字母数字下划线，密码6-20位至少包含字母和数字"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功",
                    content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在")
    })
    @PostMapping("/register")
    public Result<AuthResponse> register(
            @Valid @RequestBody @Parameter(description = "注册请求参数", required = true) RegisterRequest request
    ) {
        log.info("用户注册请求: {}", request.getUsername());
        AuthResponse response = userService.register(request);
        return Result.success("注册成功", response);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证响应
     */
    @Operation(
            summary = "用户登录",
            description = "使用用户名和密码登录，成功返回JWT令牌"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "403", description = "账号已被禁用")
    })
    @PostMapping("/login")
    public Result<AuthResponse> login(
            @Valid @RequestBody @Parameter(description = "登录请求参数", required = true) LoginRequest request
    ) {
        log.info("用户登录请求: {}", request.getUsername());
        AuthResponse response = userService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 刷新令牌
     *
     * @param request 刷新令牌请求
     * @return 新的认证响应
     */
    @Operation(
            summary = "刷新访问令牌",
            description = "使用刷新令牌获取新的访问令牌"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "刷新成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "刷新令牌无效或已过期")
    })
    @PostMapping("/refresh")
    public Result<AuthResponse> refreshToken(
            @Valid @RequestBody @Parameter(description = "刷新令牌请求", required = true) RefreshTokenRequest request
    ) {
        log.info("刷新令牌请求");
        AuthResponse response = userService.refreshToken(request.getRefreshToken());
        return Result.success("刷新成功", response);
    }

    /**
     * 验证令牌有效性
     *
     * @return 用户信息
     */
    @Operation(
            summary = "验证令牌",
            description = "验证当前请求中的JWT令牌是否有效，返回用户信息"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "令牌有效"),
            @ApiResponse(responseCode = "401", description = "令牌无效或已过期")
    })
    @GetMapping("/validate")
    public Result<AuthResponse> validate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("令牌验证失败: 用户未认证");
            return Result.error(401, "令牌无效");
        }

        String username = authentication.getName();
        if (!org.springframework.util.StringUtils.hasText(username) || "anonymousUser".equals(username)) {
            log.warn("令牌验证失败: 无效用户");
            return Result.error(401, "令牌无效");
        }

        log.info("令牌验证成功: {}", username);

        // 构建响应
        AuthResponse authResponse = AuthResponse.builder()
                .username(username)
                .user(AuthResponse.UserInfo.builder()
                        .username(username)
                        .build())
                .expiry(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                .build();

        return Result.success("令牌有效", authResponse);
    }

    /**
     * 用户登出
     *
     * @return 操作结果
     */
    @Operation(
            summary = "用户登出",
            description = "清除当前用户的认证信息"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public Result<Void> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            log.info("用户登出: {}", authentication.getName());
            SecurityContextHolder.clearContext();
        }
        return Result.success("登出成功", null);
    }
}
