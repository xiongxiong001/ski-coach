package com.skicoach.backend.common.util;

import com.skicoach.backend.common.exception.AuthException;
import com.skicoach.backend.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 *
 * 设计要点:
 * 1. 用户端和管理端使用不同的密钥(从源头隔离)
 * 2. Token里只放最少的信息(userId / adminId + role)
 * 3. 过期时间通过配置控制
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${ski.jwt.user-secret}")
    private String userSecret;

    @Value("${ski.jwt.admin-secret}")
    private String adminSecret;

    @Value("${ski.jwt.expire-hours:168}")
    private long expireHours;

    /** Token类型 */
    public enum TokenType {
        USER, ADMIN
    }

    /** Token中的claim key */
    public static final String CLAIM_ID = "id";
    public static final String CLAIM_TYPE = "type";

    // -----------------------------------------------------------
    // 生成 Token
    // -----------------------------------------------------------

    /** 生成用户Token */
    public String generateUserToken(Long userId) {
        return generateToken(userId, TokenType.USER);
    }

    /** 生成管理员Token */
    public String generateAdminToken(Long adminId) {
        return generateToken(adminId, TokenType.ADMIN);
    }

    private String generateToken(Long id, TokenType type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ID, id);
        claims.put(CLAIM_TYPE, type.name());

        long now = System.currentTimeMillis();
        long expireMillis = expireHours * 3600_000L;

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMillis))
                .signWith(getSecretKey(type))
                .compact();
    }

    // -----------------------------------------------------------
    // 解析 Token
    // -----------------------------------------------------------

    /** 解析用户Token,返回userId,失败抛AuthException */
    public Long parseUserToken(String token) {
        return parseToken(token, TokenType.USER);
    }

    /** 解析管理员Token,返回adminId */
    public Long parseAdminToken(String token) {
        return parseToken(token, TokenType.ADMIN);
    }

    private Long parseToken(String token, TokenType expectedType) {
        if (token == null || token.isEmpty()) {
            throw new AuthException(ResultCode.UNAUTHORIZED, "Token为空");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey(expectedType))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 校验类型
            String typeInToken = claims.get(CLAIM_TYPE, String.class);
            if (!expectedType.name().equals(typeInToken)) {
                throw new AuthException(ResultCode.TOKEN_INVALID, "Token类型不匹配");
            }

            // 取出ID
            Object idObj = claims.get(CLAIM_ID);
            if (idObj == null) {
                throw new AuthException(ResultCode.TOKEN_INVALID, "Token缺少必要信息");
            }
            return Long.parseLong(idObj.toString());

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new AuthException(ResultCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            throw new AuthException(ResultCode.TOKEN_INVALID);
        }
    }

    /** 获取Token剩余有效期(毫秒),用于黑名单TTL */
    public long getRemainingMillis(String token, TokenType type) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey(type))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            long expireAt = claims.getExpiration().getTime();
            long remaining = expireAt - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    // -----------------------------------------------------------
    // 内部工具
    // -----------------------------------------------------------

    private SecretKey getSecretKey(TokenType type) {
        String secret = (type == TokenType.USER) ? userSecret : adminSecret;
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT密钥长度必须>=32位,当前: " +
                    (secret == null ? 0 : secret.length()));
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
