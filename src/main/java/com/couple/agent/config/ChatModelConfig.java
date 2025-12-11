package com.couple.agent.config;

import com.couple.agent.store.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 聊天模型配置类
 配置 AI 聊天模型和聊天记忆提供者
 */
@Configuration
public class ChatModelConfig {

    /**
     * Redis 聊天记忆存储
     */
    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 创建 Azure OpenAI 聊天模型 Bean
     * @param apiKey API密钥
     * @param endPoint 端点地址
     * @param deploymentName 部署名称
     * @param temperature 温度参数
     * @param timeout 超时时间
     * @return Azure OpenAI 聊天模型实例
     */
    @Bean
   public ChatModel azureOpenAiChatModel(
           @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.base-url}") String endPoint,
            @Value("${ai.openai.deployment-name}") String deploymentName,
            @Value("${ai.openai.temperature}") double temperature,
            @Value("${ai.openai.timeout}") Duration timeout) {
        // 1. 使用与流式模型一致的配置前缀 ai.openai.*，避免占位符缺失导致 Bean 创建失败
        return AzureOpenAiChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endPoint)
                .deploymentName(deploymentName)
                .temperature(temperature)
                .timeout(timeout)
                .logRequestsAndResponses(true)
                .build();
    }

    @Bean("azureOpenAiStreamingChatModel")
    public StreamingChatModel azureOpenAiStreamingChatModel(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.base-url}") String endPoint,
            @Value("${ai.openai.deployment-name}") String deploymentName,
            @Value("${ai.openai.temperature}") double temperature,
            @Value("${ai.openai.timeout}") Duration timeout) {

        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endPoint)
                .deploymentName(deploymentName)
                .temperature(temperature)
                .timeout(timeout)
                .logRequestsAndResponses(true)
                .build();
    }

    @Bean("deepSeekStreamingChatModel")
    public StreamingChatModel deepSeekStreamingChatModel(
            @Value("${ai.deepseek.api-key}") String apiKey,
            @Value("${ai.deepseek.base-url}") String baseUrl,
            @Value("${ai.deepseek.model-name}") String modelName,
            @Value("${ai.deepseek.temperature}") double temperature,
            @Value("${ai.deepseek.timeout}") Duration timeout) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(timeout)
                .build();
    }

    @Bean("deepSeekChatModel")
    public ChatModel deepSeekChatModel(
            @Value("${ai.deepseek.api-key}") String apiKey,
            @Value("${ai.deepseek.base-url}") String baseUrl,
            @Value("${ai.deepseek.model-name}") String modelName,
            @Value("${ai.deepseek.temperature}") double temperature,
            @Value("${ai.deepseek.timeout}") Duration timeout) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(timeout)
                .build();
    }


    /**
     * 创建聊天记忆提供者 Bean
     * 使用 Redis 作为记忆存储，滑动窗口大小为10条消息
     *
     * @return 聊天记忆提供者
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }
}
