package com.couple.agent.model.vo;

import com.couple.agent.model.domain.Message;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息返回视图，对前端与 WebSocket 推送保持统一结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息返回视图")
public class MessageVo {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "发送者用户ID")
    private Long senderId;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型")
    private String contentType;


    @Schema(description = "父节点id，没有则为空")
    private Long parentId;

    @Schema(description = "子节点集合")
    private List<MessageVo> childList;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
