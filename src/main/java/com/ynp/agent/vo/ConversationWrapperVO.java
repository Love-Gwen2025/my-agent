package com.ynp.agent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 单个会话响应 VO。
 */
@Data
@AllArgsConstructor
public class ConversationWrapperVO {
    private ConversationVO conversation;
}
