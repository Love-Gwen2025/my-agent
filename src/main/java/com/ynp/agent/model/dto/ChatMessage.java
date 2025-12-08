package com.ynp.agent.model.dto;

import lombok.Data;

@Data
public class ChatMessage {
    private String content;   // 文本
    private long ts;          // 时间戳
    private String from;      // 谁发的
}