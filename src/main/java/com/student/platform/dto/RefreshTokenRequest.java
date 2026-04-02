package com.student.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求
 * <p>
 * 用于刷新访问令牌的请求参数
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
@Data
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequest {

    /**
     * 刷新令牌
     */
    @NotBlank(message = "刷新令牌不能为空")
    @Schema(description = "刷新令牌", required = true, example = "eyJhbGciOiJIUzI1NiIs...")
    private String refreshToken;
}
