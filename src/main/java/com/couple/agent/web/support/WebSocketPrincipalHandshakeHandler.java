package com.couple.agent.web.support;

import com.couple.agent.model.domain.CurrentSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
/**
 * 自定义握手处理器：根据拦截器注入的会话信息生成 Principal，确保 convertAndSendToUser 能找到唯一用户。
 */
@Component
public class WebSocketPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object sessionObj = attributes.get(WebSocketAuthHandshakeInterceptor.ATTR_CURRENT_SESSION);
        if (sessionObj instanceof CurrentSession currentSession) {
            return new WebSocketUserPrincipal(String.valueOf(currentSession.getId()), currentSession);
        }
        return super.determineUser(request, wsHandler, attributes);
    }
}
