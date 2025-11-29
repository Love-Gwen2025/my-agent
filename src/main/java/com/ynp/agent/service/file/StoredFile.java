package com.ynp.agent.service.file;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 上传结果模型。
 */
@Data
@AllArgsConstructor
public class StoredFile {
    private String originalName;
    private String storedName;
    private String mimeType;
    private long size;
}
