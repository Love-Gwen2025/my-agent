package com.couple.agent.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionView {

    /**
     * 1. 是否已登录。
     */
    private boolean authenticated;

    /**
     * 1. 助手展示名称，前端用来显示标题。
     */
    private String assistantName;

    /**
     * 1. 当前登录用户信息；未登录时为空。
     */
    private UserView user;

    /**
     * 1. 允许登录的账号提示列表。
     */
    private List<AccountView> accounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountView {
        /**
         * 1. 登录用户名，兼容前端 form 字段。
         */
        private String username;
        /**
         * 1. 展示名称。
         */
        @JsonProperty("displayName")
        private String displayName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserView {
        /**
         * 1. 登录用户名。
         */
        private String username;
        /**
         * 1. 展示名称。
         */
        @JsonProperty("displayName")
        private String displayName;
    }
}
