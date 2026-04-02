package com.student.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应DTO
 * <p>
 * 用户登录、注册成功后返回的认证信息
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "认证响应")
public class AuthResponse {

    /**
     * JWT访问令牌
     */
    @Schema(description = "JWT访问令牌", example = "eyJhbGciOiJIUzI1NiIs...")
    private String token;

    /**
     * 令牌类型
     */
    @Schema(description = "令牌类型", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    /**
     * 用户角色
     */
    @Schema(description = "用户角色", example = "STUDENT")
    private String role;

    /**
     * 用户信息
     */
    @Schema(description = "用户信息")
    private UserInfo user;

    /**
     * 令牌过期时间戳(毫秒)
     */
    @Schema(description = "令牌过期时间戳", example = "1700000000000")
    private Long expiry;

    /**
     * 刷新令牌(可选)
     */
    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIs...")
    private String refreshToken;

    /**
     * 用户信息内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "用户信息")
    public static class UserInfo {

        /**
         * 用户ID
         */
        @Schema(description = "用户ID", example = "1")
        private Long id;

        /**
         * 用户名
         */
        @Schema(description = "用户名", example = "zhangsan")
        private String username;

        /**
         * 用户角色
         */
        @Schema(description = "用户角色", example = "STUDENT")
        private String role;

        /**
         * 用户昵称
         */
        @Schema(description = "用户昵称", example = "张三")
        private String nickname;

        /**
         * 用户邮箱
         */
        @Schema(description = "用户邮箱", example = "zhangsan@example.com")
        private String email;

        /**
         * 用户手机号
         */
        @Schema(description = "用户手机号", example = "13800138000")
        private String phone;
    }
}
