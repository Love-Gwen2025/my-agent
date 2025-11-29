package com.ynp.agent.helper;

import com.ynp.agent.helper.model.JwtPayload;

/**
 * 每请求用户上下文。
 */
public final class UserContextHolder {

    private static final ThreadLocal<JwtPayload> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /**
     * 1. 写入上下文。
     */
    public static void setContext(JwtPayload payload) {
        CONTEXT.set(payload);
    }

    /**
     * 1. 获取上下文。
     */
    public static JwtPayload getContext() {
        return CONTEXT.get();
    }

    /**
     * 1. 清理上下文。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
