package com.ynp.agent.domain.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * AI 对话缓存文档，便于快速读取上下文。
 */
@Data
@Document(collection = "conversation_cache")
public class AiCacheDocument {

    @Id
    private String id;

    private Long conversationId;

    private Long userId;

    private List<String> messages;

    private Instant updatedAt;
}
