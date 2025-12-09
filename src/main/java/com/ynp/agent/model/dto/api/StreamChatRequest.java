package com.ynp.agent.model.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式聊天请求 DTO
 *
 * @author ynp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "流式聊天请求")
public class StreamChatRequest {

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    @Schema(description = "会话ID", required = true)
    private Long conversationId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户消息内容", required = true)
    private String content;

    /**
     * 模型编码
     * 如果不指定，使用会话默认模型或系统默认模型
     */
    @Schema(description = "模型编码（可选）", example = "gpt-4o")
    private String modelCode;

    /**
     * 系统提示词（可选）
     */
    @Schema(description = "系统提示词（可选）")
    private String systemPrompt;
}
