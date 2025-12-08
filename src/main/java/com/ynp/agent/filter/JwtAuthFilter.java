package com.ynp.agent.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ynp.agent.config.JwtProperties;
import com.ynp.agent.config.SecurityIgnoreProperties;
import com.ynp.agent.model.domain.CurrentSession;
import com.ynp.agent.utils.JwtUtil;
import com.ynp.agent.utils.SessionUtil;
import com.ynp.agent.utils.StringUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
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
        //在白名单之中，直接放行
        if(StringUtil.match(request.getServletPath(),securityIgnoreProperties.getWhites())){
            filterChain.doFilter(request,response);
            return;
        }
        //不在白名单中，校验token

        //1.获取token
        String token =request.getHeader("token");
        if(StringUtil.isBlank(token)){
            throwsUnValidToken(response);
            return;
        }
        //2.解析token
        Claims claims = jwtUtil.parseToken(token);
        if (Objects.isNull(claims)){
            throwsUnValidToken(response);
            return;
        }
        /*
        *3.从redis中取session判断登录状态。
        * 如果session不存在，即从zset中移除
        * */
        String userId = String.valueOf(claims.get("userId"));
        String sessionJson = redisTemplate.opsForValue()
                .get(CurrentSession.sessionKey(userId,token));
        if(!StringUtils.hasText(sessionJson)){
            redisTemplate.opsForZSet().remove(CurrentSession.indexKey(userId), token);
            throwsUnValidToken(response);
            return;
        }
        CurrentSession currentSession;
        /*
        * 如果session无法反序列化，也直接删了，多余的token会在下一次访问时删除
        * */
        try {
            currentSession = objectMapper.readValue(sessionJson, CurrentSession.class);
        } catch (JsonProcessingException e) {
            redisTemplate.opsForValue().getOperations().delete(CurrentSession.sessionKey(userId, token));
            throwsUnValidToken(response);
            return;
        }
        if (!StringUtil.equal(token, currentSession.getToken())){
            throwsUnValidToken(response);
            return;
        }
        if (!StringUtil.equal(userId, String.valueOf(currentSession.getId()))){
            throwsUnValidToken(response);
            return;
        }

        //校验全通过，刷新登陆的token有效期
        String userIdKey = String.valueOf(currentSession.getId());
        String indexKey = CurrentSession.indexKey(userIdKey);
        String sessionKey = CurrentSession.sessionKey(userIdKey, currentSession.getToken());
        long ttlSeconds = Duration.ofMinutes(jwtProperties.getExpirationMinutes()).getSeconds();
        if (ttlSeconds <= 0) {
            // 兜底处理，避免配置异常导致不过期
            ttlSeconds = 60;
        }
        double score = (double) System.currentTimeMillis();
        redisTemplate.opsForZSet().add(indexKey, sessionKey, score);
        redisTemplate.expire(indexKey, Duration.ofSeconds(ttlSeconds));
        redisTemplate.opsForValue().set(sessionKey, sessionJson, Duration.ofSeconds(ttlSeconds));

        try {
            SessionUtil.set(currentSession);
            //整个调用链
            filterChain.doFilter(request, response);
        } finally {
            //调用链结束清除
            SessionUtil.clear();
        }
    }
    private void throwsUnValidToken(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().append("token缺失或非法");
    }
}
