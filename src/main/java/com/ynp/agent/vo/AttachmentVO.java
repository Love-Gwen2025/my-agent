package com.ynp.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件 VO。
 */
@Data
public class AttachmentVO {
    private Long id;
    private String fileName;
    private String mimeType;
    private String url;
    private Long size;
    private LocalDateTime createdAt;
}
