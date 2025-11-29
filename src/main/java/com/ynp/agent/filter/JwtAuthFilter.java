package com.ynp.agent.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynp.agent.helper.AuthConstants;
import com.ynp.agent.helper.UserContextHolder;
import com.ynp.agent.helper.model.JwtPayload;
import com.ynp.agent.service.AuthService;
import com.ynp.agent.vo.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * JWT 鉴权过滤器。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    /**
     * 1. 判断是否跳过鉴权。
     */
    private boolean shouldSkip(String uri, String method) {
        return "OPTIONS".equalsIgnoreCase(method)
                || MATCHER.match("/agent/user/login", uri)
                || MATCHER.match("/agent/health", uri)
                || MATCHER.match("/agent/uploads/**", uri)
                || MATCHER.match("/error", uri);
    }

    /**
     * 1. 过滤请求，校验 JWT 并写入上下文。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        try {
            if (shouldSkip(uri, request.getMethod())) {
                /* 1. 放行无需鉴权的请求 */
                filterChain.doFilter(request, response);
                return;
            }
            /* 2. 提取并校验 JWT */
            String token = extractToken(request);
            JwtPayload payload = authService.validateToken(token);
            if (Objects.isNull(payload)) {
                if (MATCHER.match("/agent/user/session", uri)) {
                    /* 3. session 查询允许未登录透传 */
                    filterChain.doFilter(request, response);
                    return;
                }
                /* 4. 未登录返回 401 */
                writeUnauthorized(response);
                return;
            }
            /* 5. 写入上下文后继续链路 */
            UserContextHolder.setContext(payload);
            filterChain.doFilter(request, response);
        } finally {
            /* 6. 清理上下文避免线程复用污染 */
            UserContextHolder.clear();
        }
    }

    /**
     * 1. 返回未登录响应。
     */
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = objectMapper.writeValueAsString(new ErrorResponse("未登录或会话已过期"));
        response.getWriter().write(body);
    }

    /**
     * 1. 从 Cookie 中提取 token。
     */
    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (Objects.isNull(cookies)) {
            return "";
        }
        return Arrays.stream(cookies)
                .filter(item -> AuthConstants.TOKEN_COOKIE.equals(item.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
    }
}
