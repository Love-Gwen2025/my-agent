package com.couple.agent.web.support;

import com.couple.agent.model.domain.CurrentSession;
import lombok.Getter;

import java.security.Principal;
import java.util.Objects;

/**
 * WebSocket 用户身份封装：既暴露用户 ID，又保留当前会话信息，方便后续扩展。
 */
@Getter
public class WebSocketUserPrincipal implements Principal {

    private final String name;
    private final CurrentSession session;

    public WebSocketUserPrincipal(String userId, CurrentSession session) {
        this.name = Objects.requireNonNull(userId, "userId 不能为空");
        this.session = session;
    }

    @Override
    public String getName() {
        return name;
    }
}
