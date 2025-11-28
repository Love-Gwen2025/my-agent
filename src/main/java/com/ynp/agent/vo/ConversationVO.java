package com.ynp.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话 VO。
 */
@Data
public class ConversationVO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
