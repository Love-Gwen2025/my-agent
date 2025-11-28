package com.ynp.agent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 聊天请求。
 */
@Data
public class ChatRequest {

    @NotNull(message = "会话 ID 不能为空")
    private Long conversationId;

    private String message;
}
