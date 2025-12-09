package com.couple.agent.service;

import com.couple.agent.model.domain.AiModel;
import com.couple.agent.model.enums.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型选择服务
 * 负责根据模型编码动态选择和创建对应的 AI 聊天模
 */
@Slf4j
@Service
public class ModelSelectorService {

    /**
     * 同步聊天模型注册表
     */
    @Autowired(required = false)
    @Qualifier("chatModelRegistry")
    private Map<String, ChatModel> chatModelRegistry;

    /**
     * 流式聊天模型注册表
     */
    @Autowired(required = false)
    @Qualifier("streamingChatModelRegistry")
    private Map<String, StreamingChatModel> streamingChatModelRegistry;

    /**
     * OpenAI API 密钥
     */
    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    /**
     * OpenAI API 基础地址
     */
    @Value("${ai.openai.base-url:}")
    private String openAiBaseUrl;

    /**
     * 动态创建的模型缓存
     */
    private final Map<String, ChatModel> dynamicChatModelCache = new ConcurrentHashMap<>();

    /**
     * 动态创建的流式模型缓存
     */
    private final Map<String, StreamingChatModel> dynamicStreamingModelCache = new ConcurrentHashMap<>();

    /**
     * 获取同步聊天模型
     *
     * <p>根据模型编码获取对应的聊天模型，支持动态创建</p>
     *
     * @param modelCode 模型编码（如 gpt-4o, claude-3-5-sonnet-20241022）
     * @return 聊天模型实例
     */
    public ChatModel getChatModel(String modelCode) {
        // 1. 先尝试从动态缓存获取
        ChatModel cached = dynamicChatModelCache.get(modelCode);
        if (Objects.nonNull(cached)) {
            return cached;
        }

        // 2. 尝试从注册表获取默认模型
        ModelProvider provider = getProviderByModelCode(modelCode);
        if (Objects.nonNull(provider) && Objects.nonNull(chatModelRegistry)) {
            ChatModel registered = chatModelRegistry.get(provider.getCode());
            if (Objects.nonNull(registered)) {
                return registered;
            }
        }

        // 3. 动态创建模型
        ChatModel model = createChatModel(modelCode);
        if (Objects.nonNull(model)) {
            dynamicChatModelCache.put(modelCode, model);
        }
        return model;
    }

    /**
     * 获取流式聊天模型
     *
     * @param modelCode 模型编码
     * @return 流式聊天模型实例
     */
    public StreamingChatModel getStreamingChatModel(String modelCode) {
        // 1. 先尝试从动态缓存获取
        StreamingChatModel cached = dynamicStreamingModelCache.get(modelCode);
        if (Objects.nonNull(cached)) {
            return cached;
        }

        // 2. 尝试从注册表获取
        ModelProvider provider = getProviderByModelCode(modelCode);
        if (Objects.nonNull(provider) && Objects.nonNull(streamingChatModelRegistry)) {
            StreamingChatModel registered = streamingChatModelRegistry.get(provider.getCode());
            if (Objects.nonNull(registered)) {
                return registered;
            }
        }

        // 3. 动态创建模型
        StreamingChatModel model = createStreamingChatModel(modelCode);
        if (Objects.nonNull(model)) {
            dynamicStreamingModelCache.put(modelCode, model);
        }
        return model;
    }

    /**
     * 根据模型配置获取同步聊天模型
     *
     * @param aiModel 模型配置实体
     * @return 聊天模型实例
     */
    public ChatModel getChatModel(AiModel aiModel) {
        return getChatModel(aiModel.getModelCode());
    }

    /**
     * 根据模型配置获取流式聊天模型
     *
     * @param aiModel 模型配置实体
     * @return 流式聊天模型实例
     */
    public StreamingChatModel getStreamingChatModel(AiModel aiModel) {
        return getStreamingChatModel(aiModel.getModelCode());
    }

    /**
     * 根据模型编码判断提供商
     *
     * @param modelCode 模型编码
     * @return 模型提供商枚举
     */
    private ModelProvider getProviderByModelCode(String modelCode) {
        if (Objects.isNull(modelCode)) {
            return null;
        }

        // 根据模型编码前缀判断提供商
        if (modelCode.startsWith("gpt-") || modelCode.startsWith("o1-")) {
            return ModelProvider.OPENAI;
        } else if (modelCode.startsWith("claude-")) {
            return ModelProvider.ANTHROPIC;
        } else if (modelCode.startsWith("gemini-")) {
            return ModelProvider.GOOGLE_GEMINI;
        }

        return null;
    }

    /**
     * 动态创建同步聊天模型
     *
     * @param modelCode 模型编码
     * @return 聊天模型实例
     */
    private ChatModel createChatModel(String modelCode) {
        ModelProvider provider = getProviderByModelCode(modelCode);
        if (Objects.isNull(provider)) {
            log.warn("无法识别模型编码对应的提供商: {}", modelCode);
            return null;
        }

        log.info("动态创建聊天模型: {} ({})", modelCode, provider.getDisplayName());

        return switch (provider) {
            case OPENAI -> createOpenAiChatModel(modelCode);
            case ANTHROPIC, GOOGLE_GEMINI, AZURE_OPENAI -> {
                log.warn("暂不支持动态创建该提供商的模型: {}", provider);
                yield null;
            }
        };
    }

    /**
     * 动态创建流式聊天模型
     *
     * @param modelCode 模型编码
     * @return 流式聊天模型实例
     */
    private StreamingChatModel createStreamingChatModel(String modelCode) {
        ModelProvider provider = getProviderByModelCode(modelCode);
        if (Objects.isNull(provider)) {
            log.warn("无法识别模型编码对应的提供商: {}", modelCode);
            return null;
        }

        log.info("动态创建流式聊天模型: {} ({})", modelCode, provider.getDisplayName());

        return switch (provider) {
            case OPENAI -> createOpenAiStreamingChatModel(modelCode);
            case ANTHROPIC, GOOGLE_GEMINI, AZURE_OPENAI -> {
                log.warn("暂不支持动态创建该提供商的流式模型: {}", provider);
                yield null;
            }
        };
    }

    /**
     * 创建 OpenAI 同步聊天模型
     *
     * @param modelCode 模型编码
     * @return OpenAI 聊天模型
     */
    private ChatModel createOpenAiChatModel(String modelCode) {
        if (Objects.isNull(openAiApiKey) || openAiApiKey.isEmpty()) {
            log.error("OpenAI API Key 未配置");
            return null;
        }

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(modelCode)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true);

        if (Objects.nonNull(openAiBaseUrl) && !openAiBaseUrl.isEmpty()) {
            builder.baseUrl(openAiBaseUrl);
        }

        return builder.build();
    }

    /**
     * 创建 OpenAI 流式聊天模型
     *
     * @param modelCode 模型编码
     * @return OpenAI 流式聊天模型
     */
    private StreamingChatModel createOpenAiStreamingChatModel(String modelCode) {
        if (Objects.isNull(openAiApiKey) || openAiApiKey.isEmpty()) {
            log.error("OpenAI API Key 未配置");
            return null;
        }

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(modelCode)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true);

        if (Objects.nonNull(openAiBaseUrl) && !openAiBaseUrl.isEmpty()) {
            builder.baseUrl(openAiBaseUrl);
        }

        return builder.build();
    }

    /**
     * 清除模型缓存
     *
     * <p>当模型配置发生变化时调用</p>
     */
    public void clearCache() {
        dynamicChatModelCache.clear();
        dynamicStreamingModelCache.clear();
        log.info("已清除模型缓存");
    }
}
