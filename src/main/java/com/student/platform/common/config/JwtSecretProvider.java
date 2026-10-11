package com.student.platform.common.config;

import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JWT 签名密钥的统一来源。
 *
 * 背景：原先 {@code application.yml} 中为 {@code jwt.secret} 写了一个硬编码兜底默认值，
 * 而 Spring Boot 并不读取仓库根目录的 .env，Dockerfile 也没有注入该变量，
 * 因此直接 {@code java -jar} / {@code docker run} 时会落到那串**公开在代码仓库里**的密钥，
 * 任何人都可以据此自签任意 userId 的 token。
 *
 * 现在的策略：
 * 1. 配置为空（未设置 JWT_SECRET）时，生成一次性随机强密钥并打印醒目告警：
 *    本地开发仍可开箱启动，但重启后旧 token 会失效，且明确提示生产必须配置。
 * 2. 配置为曾经写死的弱默认值时，直接拒绝启动（fail fast），避免带病上线。
 * 3. 配置了自定义密钥但长度不足 32 字节时，同样拒绝启动（jjwt 对 HS256 的最低要求）。
 *
 * 必须保证签发方（JwtUtil）与校验方（JwtAuthFilter）拿到的是同一个密钥实例，
 * 因此这里以单例 Bean 形式提供，禁止各处自行用 @Value 解析。
 */
@Slf4j
@Component
public class JwtSecretProvider {

    /** HS256 要求的最小密钥长度（字节） */
    private static final int MIN_SECRET_BYTES = 32;

    /** 曾经写死在 application.yml 中的默认值，出现即视为配置错误 */
    private static final String LEGACY_WEAK_SECRET =
            "change-me-dev-only-student-platform-jwt-secret-32-bytes-minimum";

    private final SecretKey secretKey;

    public JwtSecretProvider(@Value("${jwt.secret:}") String configured) {
        this.secretKey = build(configured);
    }

    /**
     * 获取统一的签名密钥。
     */
    public SecretKey get() {
        return secretKey;
    }

    private SecretKey build(String configured) {
        String secret = configured == null ? "" : configured.trim();

        if (secret.isEmpty()) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            String generated = Base64.getEncoder().encodeToString(random);
            log.warn("==============================================================");
            log.warn("未配置 JWT_SECRET，已生成一次性随机密钥用于本地开发。");
            log.warn("影响：服务重启后所有已签发 token 将失效，需要重新登录。");
            log.warn("生产环境务必通过环境变量 JWT_SECRET 配置不少于 32 字节的强密钥。");
            log.warn("==============================================================");
            return Keys.hmacShaKeyFor(generated.getBytes(StandardCharsets.UTF_8));
        }

        if (LEGACY_WEAK_SECRET.equalsIgnoreCase(secret)) {
            throw new IllegalStateException(
                    "检测到 JWT_SECRET 仍使用代码中曾硬编码的弱默认密钥，已拒绝启动。"
                            + "请通过环境变量 JWT_SECRET 配置不少于 32 字节的强随机密钥。");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET 强度不足：当前 " + keyBytes.length + " 字节，至少需要 "
                            + MIN_SECRET_BYTES + " 字节。请重新配置后启动。");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
