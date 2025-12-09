package com.couple.agent.web.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.couple.agent.config.JwtProperties;
import com.couple.agent.model.domain.CurrentSession;
import com.couple.agent.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * WebSocket 握手拦截器：在建立 STOMP 连接前完成 token 校验，并把当前会话信息放入属性，供后续 Principal 使用。
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 握手阶段在属性中存储的键，供 {@link WebSocketPrincipalHandshakeHandler} 读取。
     */
    public static final String ATTR_CURRENT_SESSION = "WS_CURRENT_SESSION";

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        // 优先读取 Header 中的 token，若前端以查询参数形式携带则兜底读取参数
        String token = httpRequest.getHeader("token");
        if (!StringUtils.hasText(token)) {
            token = httpRequest.getParameter("token");
        }
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            if (Objects.isNull(claims)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            String userId = String.valueOf(claims.get("userId"));
            if (!StringUtils.hasText(userId)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            String sessionKey = CurrentSession.sessionKey(userId, token);
            String indexKey = CurrentSession.indexKey(userId);
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            if (!StringUtils.hasText(sessionJson)) {
                redisTemplate.opsForZSet().remove(indexKey, sessionKey);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            CurrentSession currentSession;
            try {
                currentSession = objectMapper.readValue(sessionJson, CurrentSession.class);
            } catch (JsonProcessingException ex) {
                redisTemplate.delete(sessionKey);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            if (!Objects.equals(token, currentSession.getToken())
                    || !Objects.equals(userId, String.valueOf(currentSession.getId()))) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 校验通过后刷新 Redis 中的会话过期时间，保持与 HTTP 链路一致
            long ttlSeconds = Duration.ofMinutes(jwtProperties.getExpirationMinutes()).getSeconds();
            if (ttlSeconds <= 0L) {
                ttlSeconds = 60L;
            }
            double score = (double) System.currentTimeMillis();
            redisTemplate.opsForZSet().add(indexKey, sessionKey, score);
            redisTemplate.expire(indexKey, Duration.ofSeconds(ttlSeconds));
            redisTemplate.opsForValue().set(sessionKey, sessionJson, Duration.ofSeconds(ttlSeconds));

            attributes.put(ATTR_CURRENT_SESSION, currentSession);
            return true;
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需额外处理
    }
}
