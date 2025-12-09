package com.couple.agent.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.couple.agent.config.JwtProperties;
import com.couple.agent.config.SecurityIgnoreProperties;
import com.couple.agent.model.domain.CurrentSession;
import com.couple.agent.utils.JwtUtil;
import com.couple.agent.utils.SessionUtil;
import com.couple.agent.utils.StringUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final JwtProperties jwtProperties;

    private final SecurityIgnoreProperties securityIgnoreProperties;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        String token = resolveToken(request);
        boolean isPublicPath = isPublic(servletPath);
        // 1. 公共路径：允许无 token 访问，但若携带合法 token 仍尝试解析以便下游获取用户信息。
        if (isPublicPath) {
            if (StringUtils.hasText(token)) {
                tryPopulateSession(token);
            }
            filterChain.doFilter(request, response);
            SessionUtil.clear();
            return;
        }
        // 2. 受保护路径：必须携带合法 token。
        if (!StringUtils.hasText(token)) {
            throwsUnValidToken(response);
            return;
        }
        CurrentSession currentSession = tryPopulateSession(token);
        if (Objects.isNull(currentSession)) {
            throwsUnValidToken(response);
            return;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SessionUtil.clear();
        }
    }
    private void throwsUnValidToken(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().append("token缺失或非法");
    }

    /**
     * 1. 解析 token 并刷新 Redis 有效期。
     * 2. 失败时返回 null，不中断调用链，由上层决定是否放行。
     */
    private CurrentSession tryPopulateSession(String token) {
        Claims claims = jwtUtil.parseToken(token);
        if (Objects.isNull(claims)) {
            return null;
        }
        String userId = String.valueOf(claims.get("userId"));
        String sessionKey = CurrentSession.sessionKey(userId, token);
        String sessionJson = redisTemplate.opsForValue().get(sessionKey);
        if (!StringUtils.hasText(sessionJson)) {
            redisTemplate.opsForZSet().remove(CurrentSession.indexKey(userId), token);
            return null;
        }
        CurrentSession currentSession;
        try {
            currentSession = objectMapper.readValue(sessionJson, CurrentSession.class);
        } catch (JsonProcessingException ex) {
            redisTemplate.opsForValue().getOperations().delete(sessionKey);
            return null;
        }
        if (!StringUtil.equal(token, currentSession.getToken())) {
            return null;
        }
        if (!StringUtil.equal(userId, String.valueOf(currentSession.getId()))) {
            return null;
        }
        long ttlSeconds = Duration.ofMinutes(jwtProperties.getExpirationMinutes()).getSeconds();
        if (ttlSeconds <= 0) {
            ttlSeconds = 60;
        }
        String indexKey = CurrentSession.indexKey(userId);
        double score = (double) System.currentTimeMillis();
        redisTemplate.opsForZSet().add(indexKey, sessionKey, score);
        redisTemplate.expire(indexKey, Duration.ofSeconds(ttlSeconds));
        redisTemplate.opsForValue().set(sessionKey, sessionJson, Duration.ofSeconds(ttlSeconds));
        SessionUtil.set(currentSession);
        return currentSession;
    }

    /**
     * 1. 计算是否为公共路径，包含配置白名单与固定的鉴权接口。
     */
    private boolean isPublic(String servletPath) {
        List<String> defaultWhites = Arrays.asList(
                "/user/login",
                "/user/register",
                "/user/logout",
                "/user/session",
                "/agent/health",
                "/couple-agent/user/login",
                "/couple-agent/user/register",
                "/couple-agent/user/logout",
                "/couple-agent/user/session",
                "/couple-agent/agent/health"
        );
        if (StringUtil.match(servletPath, securityIgnoreProperties.getWhites())) {
            return true;
        }
        return StringUtil.match(servletPath, defaultWhites);
    }

    /**
     * 1. 从 Header 或 Cookie 中提取 token，优先 Header。
     */
    private String resolveToken(HttpServletRequest request) {
        String headerToken = request.getHeader("token");
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }
        Cookie[] cookies = request.getCookies();
        if (Objects.isNull(cookies) || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (Objects.isNull(cookie)) {
                continue;
            }
            if (Objects.equals(cookie.getName(), "token") && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
