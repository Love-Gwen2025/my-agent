package com.ynp.agent.helper;

import com.ynp.agent.config.AppProperties;
import com.ynp.agent.helper.model.JwtPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

/**
 * JWT 工具类。
 */
@Component
public class JwtHelper {

    private final AppProperties appProperties;

    public JwtHelper(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * 1. 生成 JWT，写入基础用户信息。
     */
    public String generateToken(Long userId, String username, String displayName) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(appProperties.getJwtExpireMinutes(), ChronoUnit.MINUTES);
        /* 1. 组装载荷并签名 */
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("displayName", displayName)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expireAt))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 1. 解析 JWT，返回载荷。
     */
    public JwtPayload parseToken(String token) {
        try {
            /* 1. 解析 JWT 获取 Claims */
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Long userId = claims.get("uid", Number.class).longValue();
            String username = claims.getSubject();
            String displayName = claims.get("displayName", String.class);
            return new JwtPayload(userId, username, displayName, claims.getExpiration().toInstant());
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 1. 生成密钥。
     */
    private Key getKey() {
        return Keys.hmacShaKeyFor(appProperties.getSessionSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 1. 校验是否即将过期，便于后续续期扩展。
     */
    public boolean isAboutToExpire(JwtPayload payload) {
        if (Objects.isNull(payload) || Objects.isNull(payload.getExpireAt())) {
            return true;
        }
        Instant now = Instant.now();
        Instant expireAt = payload.getExpireAt();
        Instant threshold = now.plus(10, ChronoUnit.MINUTES);
        /* 1. 预判是否临近过期 */
        return expireAt.isBefore(threshold);
    }
}
