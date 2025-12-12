package com.couple.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 会话历史树形视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话历史树形视图")
public class ConversationHistoryVo {

    @Schema(description = "树形历史消息列表")
    private List<MessageVo> messages = Collections.emptyList();

    @Schema(description = "当前活跃的消息ID，用于标记默认分支")
    private Long currentMessageId;
}
