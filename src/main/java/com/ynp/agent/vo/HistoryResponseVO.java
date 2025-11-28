package com.ynp.agent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 历史响应 VO。
 */
@Data
@AllArgsConstructor
public class HistoryResponseVO {
    private ConversationVO conversation;
    private List<HistoryMessageVO> history;
}
