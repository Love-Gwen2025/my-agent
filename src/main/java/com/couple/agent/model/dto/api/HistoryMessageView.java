package com.couple.agent.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryMessageView {

    /**
     * 1. 角色标识：user 或 assistant。
     */
    private String role;

    /**
     * 1. 消息正文。
     */
    private String content;

    /**
     * 1. 创建时间，前端按 created_at 解析。
     */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    /**
     * 1. 附件列表，当前返回空列表占位，兼容前端解析。
     */
    private List<Object> attachments = Collections.emptyList();
}
