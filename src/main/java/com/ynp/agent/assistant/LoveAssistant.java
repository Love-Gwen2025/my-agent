package com.ynp.agent.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "azureOpenAiChatModel",
        chatMemoryProvider = "chatMemoryProvider"
)
public interface LoveAssistant {
    @SystemMessage("你是一名情感分析大师，擅长从各种细节入手分析当事人的心情")
    String chat(@MemoryId int memoryId, @UserMessage String userMessage);
}
