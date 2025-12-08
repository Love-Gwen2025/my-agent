package com.ynp.agent.model.dto.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天回复视图，兼容前端对 /api/chat 的返回使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReplyView {

    /**
     * 1. 助手回复内容。
     */
    private String reply;
}
