package com.ynp.agent.config;

import com.ynp.agent.model.enums.ModelProvider;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多模型配置类
 *
 * <p>配置多个 AI 模型提供商的聊天模型，支持：</p>
 * <ul>
 *     <li>OpenAI (GPT-4o, GPT-3.5-turbo 等)</li>
 *     <li>Anthropic Claude (Claude 3.5 Sonnet, Opus 等)</li>
 *     <li>Google Gemini (Gemini 1.5 Pro 等)</li>
 * </ul>
 *
 * @author ynp
 */
@Slf4j
@Configuration
public class MultiModelConfig {

    /**
     * 默认超时时间（60秒）
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /**
     * 默认温度参数
     */
    private static final Double DEFAULT_TEMPERATURE = 0.7;

    // ==================== OpenAI 配置 ====================

    /**
     * 创建 OpenAI 聊天模型（同步）
     *
     * @param apiKey API 密钥
     * @param baseUrl API 基础地址（可选，用于代理）
     * @return OpenAI 聊天模型
     */
    @Bean("openAiChatModel")
    @ConditionalOnProperty(name = "ai.openai.enabled", havingValue = "true", matchIfMissing = false)
    public ChatModel openAiChatModel(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.base-url:}") String baseUrl) {

        log.info("初始化 OpenAI 聊天模型");

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o")
                .temperature(DEFAULT_TEMPERATURE)
                .timeout(DEFAULT_TIMEOUT)
                .logRequests(true)
                .logResponses(true);

        // 如果配置了代理地址，则使用代理
        if (Objects.nonNull(baseUrl) && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }

        return builder.build();
    }

    /**
     * 创建 OpenAI 流式聊天模型
     *
     * @param apiKey API 密钥
     * @param baseUrl API 基础地址（可选）
     * @return OpenAI 流式聊天模型
     */
    @Bean("openAiStreamingChatModel")
    @ConditionalOnProperty(name = "ai.openai.enabled", havingValue = "true", matchIfMissing = false)
    public StreamingChatModel openAiStreamingChatModel(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.base-url:}") String baseUrl) {

        log.info("初始化 OpenAI 流式聊天模型");

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o")
                .temperature(DEFAULT_TEMPERATURE)
                .timeout(DEFAULT_TIMEOUT)
                .logRequests(true)
                .logResponses(true);

        if (Objects.nonNull(baseUrl) && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }

        return builder.build();
    }

    // ==================== Anthropic Claude 配置 ====================

    /**
     * 创建 Anthropic Claude 聊天模型（同步）
     *
     * @param apiKey API 密钥
     * @return Claude 聊天模型
     */
    @Bean("anthropicChatModel")
    @ConditionalOnProperty(name = "ai.anthropic.enabled", havingValue = "true", matchIfMissing = false)
    public ChatModel anthropicChatModel(
            @Value("${ai.anthropic.api-key}") String apiKey) {

        log.info("初始化 Anthropic Claude 聊天模型");

        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName("claude-3-5-sonnet-20241022")
                .temperature(DEFAULT_TEMPERATURE)
                .timeout(DEFAULT_TIMEOUT)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 创建 Anthropic Claude 流式聊天模型
     *
     * @param apiKey API 密钥
     * @return Claude 流式聊天模型
     */
    @Bean("anthropicStreamingChatModel")
    @ConditionalOnProperty(name = "ai.anthropic.enabled", havingValue = "true", matchIfMissing = false)
    public StreamingChatModel anthropicStreamingChatModel(
            @Value("${ai.anthropic.api-key}") String apiKey) {

        log.info("初始化 Anthropic Claude 流式聊天模型");

        return AnthropicStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("claude-3-5-sonnet-20241022")
                .temperature(DEFAULT_TEMPERATURE)
                .timeout(DEFAULT_TIMEOUT)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // ==================== Google Gemini 配置 ====================

    /**
     * 创建 Google Gemini 聊天模型（同步）
     *
     * @param apiKey API 密钥
     * @return Gemini 聊天模型
     */
    @Bean("geminiChatModel")
    @ConditionalOnProperty(name = "ai.gemini.enabled", havingValue = "true", matchIfMissing = false)
    public ChatModel geminiChatModel(
            @Value("${ai.gemini.api-key}") String apiKey) {

        log.info("初始化 Google Gemini 聊天模型");

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-pro")
                .temperature(DEFAULT_TEMPERATURE)
                .timeout(DEFAULT_TIMEOUT)
                .logRequestsAndResponses(true)
                .build();
    }

    /**
     * 创建 Google Gemini 流式聊天模型
     *
     * @param apiKey API 密钥
     * @return Gemini 流式聊天模型
     */
    @Bean("geminiStreamingChatModel")
    @ConditionalOnProperty(name = "ai.gemini.enabled", havingValue = "true", matchIfMissing = false)
    public StreamingChatModel geminiStreamingChatModel(
            @Value("${ai.gemini.api-key}") String apiKey) {

        log.info("初始化 Google Gemini 流式聊天模型");

        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-pro")
                .temperature(DEFAULT_TEMPERATURE)
                .timeout(DEFAULT_TIMEOUT)
                .logRequestsAndResponses(true)
                .build();
    }

    // ==================== 模型注册表 ====================

    /**
     * 创建同步聊天模型注册表
     *
     * <p>将所有可用的聊天模型注册到 Map 中，便于动态选择</p>
     *
     * @param models 所有可用的聊天模型
     * @return 模型注册表（提供商编码 -> 模型实例）
     */
    @Bean("chatModelRegistry")
    public Map<String, ChatModel> chatModelRegistry(
            Map<String, ChatModel> models) {

        Map<String, ChatModel> registry = new ConcurrentHashMap<>();

        models.forEach((beanName, model) -> {
            // 根据 Bean 名称确定提供商
            if (beanName.contains("openAi")) {
                registry.put(ModelProvider.OPENAI.getCode(), model);
            } else if (beanName.contains("anthropic")) {
                registry.put(ModelProvider.ANTHROPIC.getCode(), model);
            } else if (beanName.contains("gemini")) {
                registry.put(ModelProvider.GOOGLE_GEMINI.getCode(), model);
            } else if (beanName.contains("azure")) {
                registry.put(ModelProvider.AZURE_OPENAI.getCode(), model);
            }
        });

        log.info("已注册 {} 个同步聊天模型: {}", registry.size(), registry.keySet());
        return registry;
    }

    /**
     * 创建流式聊天模型注册表
     *
     * @param models 所有可用的流式聊天模型
     * @return 流式模型注册表
     */
    @Bean("streamingChatModelRegistry")
    public Map<String, StreamingChatModel> streamingChatModelRegistry(
            Map<String, StreamingChatModel> models) {

        Map<String, StreamingChatModel> registry = new ConcurrentHashMap<>();

        models.forEach((beanName, model) -> {
            if (beanName.contains("openAi")) {
                registry.put(ModelProvider.OPENAI.getCode(), model);
            } else if (beanName.contains("anthropic")) {
                registry.put(ModelProvider.ANTHROPIC.getCode(), model);
            } else if (beanName.contains("gemini")) {
                registry.put(ModelProvider.GOOGLE_GEMINI.getCode(), model);
            } else if (beanName.contains("azure")) {
                registry.put(ModelProvider.AZURE_OPENAI.getCode(), model);
            }
        });

        log.info("已注册 {} 个流式聊天模型: {}", registry.size(), registry.keySet());
        return registry;
    }
}
