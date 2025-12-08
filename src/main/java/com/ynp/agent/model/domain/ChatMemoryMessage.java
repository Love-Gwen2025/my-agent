package com.ynp.agent.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@AllArgsConstructor
@NoArgsConstructor
@Document("chat_memory_message")
@Data
public class ChatMemoryMessage {
    @Id
    private ObjectId messageId;
    private String memoryId;
    private String content;
}
