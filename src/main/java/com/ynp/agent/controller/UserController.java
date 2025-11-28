package com.ynp.agent.controller;

import com.ynp.agent.dto.LoginRequest;
import com.ynp.agent.helper.AuthConstants;
import com.ynp.agent.helper.UserContextHolder;
import com.ynp.agent.helper.model.JwtPayload;
import com.ynp.agent.config.AppProperties;
import com.ynp.agent.service.AuthService;
import com.ynp.agent.vo.MessageVO;
import com.ynp.agent.vo.SessionVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/**
 * 用户与鉴权接口。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final AuthService authService;
    private final AppProperties appProperties;

    public UserController(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.appProperties = appProperties;
    }

    /**
     * 1. 获取当前登录态。
     */
    @GetMapping("/session")
    public ResponseEntity<SessionVO> session() {
        JwtPayload payload = UserContextHolder.getContext();
        /* 1. 根据上下文生成会话视图 */
        SessionVO vo = authService.fetchSession(payload);
        return ResponseEntity.ok(vo);
    }

    /**
     * 1. 登录并返回提示。
     */
    @PostMapping("/login")
    public ResponseEntity<MessageVO> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        /* 1. 执行登录并写入 Cookie */
        String token = authService.login(request.getUsername(), request.getPassword());
        ResponseCookie cookie = buildCookie(token, false);
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok(new MessageVO("登录成功"));
    }

    /**
     * 1. 登出并清理 Cookie。
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageVO> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractToken(request);
        /* 1. 清理 token 并下发过期 Cookie */
        authService.logout(token);
        ResponseCookie expired = buildCookie("", true);
        response.addHeader("Set-Cookie", expired.toString());
        return ResponseEntity.ok(new MessageVO("已退出登录"));
    }

    /**
     * 1. 构造 Cookie。
     */
    private ResponseCookie buildCookie(String token, boolean expireNow) {
        Duration maxAge = expireNow ? Duration.ZERO : Duration.ofMinutes(appProperties.getJwtExpireMinutes());
        boolean secure = "production".equalsIgnoreCase(System.getenv("NODE_ENV"));
        return ResponseCookie.from(AuthConstants.TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .path("/agent")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }

    /**
     * 1. 从 Cookie 提取 token。
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
