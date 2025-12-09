package com.couple.agent.config;

import com.couple.agent.store.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 聊天模型配置类
 *
 * <p>配置 AI 聊天模型和聊天记忆提供者</p>
 *
 * @author ynp
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
     *
     * @param apiKey API密钥
     * @param endPoint 端点地址
     * @param deploymentName 部署名称
     * @param temperature 温度参数
     * @param timeout 超时时间
     * @return Azure OpenAI 聊天模型实例
     */

//    public ChatModel azureOpenAiChatModel(
//            @Value("${openai.api_key}") String apiKey,
//            @Value("${openai.base_url}") String endPoint,
//            @Value("${openai.deployment_name}") String deploymentName,
//            @Value("${openai.temperature}") double temperature,
//            @Value("${openai.timeout}") Duration timeout) {
//
//        return AzureOpenAiChatModel.builder()
//                .apiKey(apiKey)
//                .endpoint(endPoint)
//                .deploymentName(deploymentName)
//                .temperature(temperature)
//                .timeout(timeout)
//                .logRequestsAndResponses(true)
//                .build();
//    }

    @Bean("azureOpenAiChatModel")
    public StreamingChatModel azureOpenAiChatModel(
            @Value("${openai.api_key}") String apiKey,
            @Value("${openai.base_url}") String endPoint,
            @Value("${openai.deployment_name}") String deploymentName,
            @Value("${openai.temperature}") double temperature,
            @Value("${openai.timeout}") Duration timeout) {

        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endPoint)
                .deploymentName(deploymentName)
                .temperature(temperature)
                .timeout(timeout)
                .logRequestsAndResponses(true)
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
