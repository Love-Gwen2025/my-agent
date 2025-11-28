package com.ynp.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会话标题请求。
 */
@Data
public class ConversationTitleRequest {

    @NotBlank(message = "标题不能为空")
    private String title;
}
