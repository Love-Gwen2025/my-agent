package com.ynp.agent.utils;


import com.ynp.agent.model.domain.CurrentSession;
import lombok.Data;

@Data
public final class SessionUtil {
    private static final ThreadLocal<CurrentSession> USER_ID_HOLDER = new ThreadLocal<>();

    public static void set(CurrentSession session) {
        USER_ID_HOLDER.set(session);
    }

    public static CurrentSession get() {
        return USER_ID_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
