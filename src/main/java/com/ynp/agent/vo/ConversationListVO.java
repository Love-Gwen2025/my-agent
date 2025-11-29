package com.ynp.agent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 会话列表响应 VO。
 */
@Data
@AllArgsConstructor
public class ConversationListVO {
    private List<ConversationVO> conversations;
}
