package com.ynp.agent.config;


import com.ynp.agent.store.MongoMemoryMessageStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ChatModelConfig {

    @Autowired
    private MongoMemoryMessageStore mongoMemoryMessageStore;

    @Bean("azureOpenAiChatModel")
    public ChatModel azureOpenAiChatModel(
            @Value("${openai.api_key}") String apiKey,
            @Value("${openai.base_url}") String endPoint,
            @Value("${openai.deployment_name}") String deploymentName,
            @Value("${openai.temperature}") double temperature,
            @Value("${openai.timeout}") Duration timeout) {

        return AzureOpenAiChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endPoint)
                .deploymentName(deploymentName)
                .temperature(temperature)
                .timeout(timeout)
                .logRequestsAndResponses(true)
                .build();
    }

    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(mongoMemoryMessageStore)
                .build();
    }

}
