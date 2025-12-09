package com.couple.agent.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationView {

    /**
     * 1. 会话唯一 ID。
     */
    private Long id;

    /**
     * 1. 会话标题。
     */
    private String title;

    /**
     * 1. 创建时间，字段名保持蛇形以兼容前端解析。
     */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    /**
     * 1. 最近更新时间，字段名保持蛇形以兼容前端解析。
     */
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
