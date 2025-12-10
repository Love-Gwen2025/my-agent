package com.couple.agent.utils;


import com.couple.agent.config.JwtProperties;
import com.couple.agent.exception.BizException;
import com.couple.agent.model.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class JwtUtil {

    private final JwtProperties properties;

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getExpirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("userId", String.valueOf(user.getId()))
                .claim("userName", user.getUserName())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public Claims parseToken(String token) {
        /**
         * 1. 构造解析器并校验签名，返回解析后的 Claims。
         * 2. 任意解析异常（过期、签名错误、格式不合法等）都记录警告并返回 null，避免抛出 RuntimeException 造成 500。
         */
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception ex) {
            log.warn("解析 JWT 失败，token 可能已过期或被篡改", ex);
            return null;
        }
    }
}
