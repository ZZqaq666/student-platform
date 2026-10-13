package com.student.platform.user.controller;

import com.student.platform.common.config.JwtBlacklist;
import com.student.platform.common.dto.AuthResponse;
import com.student.platform.common.dto.LoginRequest;
import com.student.platform.common.dto.RefreshTokenRequest;
import com.student.platform.common.dto.RegisterRequest;
import com.student.platform.common.result.Result;
import com.student.platform.common.util.JwtUtil;
import com.student.platform.user.service.UserService;
import io.jsonwebtoken.JwtException;
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
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户认证", description = "用户注册、登录、令牌刷新等认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final JwtBlacklist jwtBlacklist;

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

    @Operation(
            summary = "验证令牌",
            description = "验证当前请求中的JWT令牌是否有效，返回用户信息"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "令牌有效"),
            @ApiResponse(responseCode = "401", description = "令牌无效或已过期")
    })
    @GetMapping("/validate")
    public Result<AuthResponse> validate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
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

        // 返回令牌自身的到期时间。原先写死 now+24h，前端据此判断续期时机会一直误判。
        long expiry = 0L;
        String token = extractBearer(authHeader);
        if (token != null) {
            try {
                expiry = jwtUtil.extractExpiration(token).getTime();
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("令牌验证成功但无法解析到期时间: {}", e.getMessage());
            }
        }

        AuthResponse authResponse = AuthResponse.builder()
                .username(username)
                .user(AuthResponse.UserInfo.builder()
                        .username(username)
                        .build())
                .expiry(expiry)
                .build();

        return Result.success("令牌有效", authResponse);
    }

    @Operation(
            summary = "用户登出",
            description = "吊销当前访问令牌与随请求提交的刷新令牌"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            log.info("用户登出: {}", authentication.getName());
            SecurityContextHolder.clearContext();
        }

        // 访问令牌与刷新令牌都要吊销：access 有效期已压到分钟级，
        // 只废 access 等于留下一个 7 天可用的续期凭证。
        revokeQuietly(extractBearer(authHeader));
        if (request != null) {
            revokeQuietly(request.getRefreshToken());
        }
        return Result.success("登出成功", null);
    }

    private void revokeQuietly(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            jwtBlacklist.revoke(jwtUtil.extractTokenId(token), jwtUtil.getExpirationTime(token));
        } catch (JwtException | IllegalArgumentException e) {
            // 令牌本身已不可解析，无从吊销也不需吊销
            log.debug("登出时令牌不可解析，跳过吊销: {}", e.getMessage());
        }
    }

    private String extractBearer(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
    }
}
