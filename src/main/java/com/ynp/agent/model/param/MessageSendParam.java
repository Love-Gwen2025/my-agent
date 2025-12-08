package com.ynp.agent.model.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 发送消息入参。
 */
@Schema(description = "消息发送参数")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MessageSendParam {

    @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "消息类型：TEXT/IMAGE/FILE 等", defaultValue = "TEXT")
    private String contentType;

    @Schema(description = "引用的消息ID，可为空")
    private Long replyTo;
}
