package com.couple.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "流式聊天事件")
public class StreamChatEvent {

    /**
     * 事件类型
     * chunk: 内容片段
     * done: 完成
     * error: 错误
     */
    @Schema(description = "事件类型：chunk/done/error")
    private String type;

    /**
     * 内容片段（type=chunk 时有值）
     */
    @Schema(description = "内容片段")
    private String content;

    /**
     * 消息ID（type=done 时返回）
     */
    @Schema(description = "消息ID")
    private Long messageId;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID")
    private Long conversationId;

    /**
     * 错误信息（type=error 时有值）
     */
    @Schema(description = "错误信息")
    private String error;

    /**
     * Token 使用量（type=done 时返回）
     */
    @Schema(description = "Token使用量")
    private Integer tokenCount;

    /**
     * 创建内容片段事件
     *
     * @param content 内容片段
     * @return 事件对象
     */
    public static StreamChatEvent chunk(String content) {
        return StreamChatEvent.builder()
                .type("chunk")
                .content(content)
                .build();
    }

    /**
     * 创建完成事件
     *
     * @param messageId 消息ID
     * @param conversationId 会话ID
     * @param tokenCount Token使用量
     * @return 事件对象
     */
    public static StreamChatEvent done(Long messageId, Long conversationId, Integer tokenCount) {
        return StreamChatEvent.builder()
                .type("done")
                .messageId(messageId)
                .conversationId(conversationId)
                .tokenCount(tokenCount)
                .build();
    }

    /**
     * 创建错误事件
     *
     * @param error 错误信息
     * @return 事件对象
     */
    public static StreamChatEvent error(String error) {
        return StreamChatEvent.builder()
                .type("error")
                .error(error)
                .build();
    }
}
