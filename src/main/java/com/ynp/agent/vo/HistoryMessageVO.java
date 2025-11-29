package com.ynp.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 历史消息 VO。
 */
@Data
public class HistoryMessageVO {
    private String role;
    private String content;
    private LocalDateTime createdAt;
    private List<AttachmentVO> attachments;
}
