package com.student.platform.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 * <p>
 * 提供JWT令牌的生成、解析和验证功能
 * 使用HS256算法进行签名
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    private static final String CLAIM_KEY_USER_ID = "userId";
    private static final String CLAIM_KEY_ROLE = "role";
    private static final String CLAIM_KEY_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * 获取签名密钥
     *
     * @return SecretKey 密钥对象
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问令牌
     *
     * @param username 用户名
     * @param userId   用户ID
     * @param role     用户角色
     * @return JWT令牌
     */
    public String generateToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USER_ID, userId);
        claims.put(CLAIM_KEY_ROLE, role);
        claims.put(CLAIM_KEY_TYPE, TOKEN_TYPE_ACCESS);
        return createToken(claims, username, expiration);
    }

    /**
     * 生成访问令牌(简化版)
     *
     * @param username 用户名
     * @return JWT令牌
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_TYPE, TOKEN_TYPE_ACCESS);
        return createToken(claims, username, expiration);
    }

    /**
     * 生成刷新令牌
     *
     * @param username 用户名
     * @param userId   用户ID
     * @return 刷新令牌
     */
    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USER_ID, userId);
        claims.put(CLAIM_KEY_TYPE, TOKEN_TYPE_REFRESH);
        return createToken(claims, username, refreshExpiration);
    }

    /**
     * 创建JWT令牌
     *
     * @param claims     自定义声明
     * @param subject    主题(用户名)
     * @param expiration 过期时间(毫秒)
     * @return JWT令牌字符串
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .id(generateTokenId())
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成唯一令牌ID
     *
     * @return 令牌ID
     */
    private String generateTokenId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从令牌中提取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     * @throws JwtException 如果令牌无效
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从令牌中提取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get(CLAIM_KEY_USER_ID);
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    /**
     * 从令牌中提取角色
     *
     * @param token JWT令牌
     * @return 角色
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return (String) claims.get(CLAIM_KEY_ROLE);
    }

    /**
     * 从令牌中提取过期时间
     *
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 提取指定声明
     *
     * @param token          JWT令牌
     * @param claimsResolver 声明解析器
     * @param <T>            返回类型
     * @return 声明值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 提取所有声明
     *
     * @param token JWT令牌
     * @return Claims对象
     * @throws JwtException 如果令牌无效或过期
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT令牌: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("格式错误的JWT令牌: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.warn("JWT签名验证失败: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT令牌为空或非法: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 检查令牌是否过期
     *
     * @param token JWT令牌
     * @return 是否过期
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 验证令牌有效性
     *
     * @param token    JWT令牌
     * @param username 用户名
     * @return 是否有效
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证令牌有效性(仅验证格式和签名)
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否为刷新令牌
     *
     * @param token JWT令牌
     * @return 是否为刷新令牌
     */
    public Boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_KEY_TYPE));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取令牌剩余有效时间(毫秒)
     *
     * @param token JWT令牌
     * @return 剩余有效时间，如果已过期返回0
     */
    public long getExpirationTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
