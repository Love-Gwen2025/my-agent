package com.ynp.agent.service.impl;

import com.ynp.agent.model.domain.Conversation;
import com.ynp.agent.model.domain.Message;
import com.ynp.agent.model.dto.api.StreamChatEvent;
import com.ynp.agent.service.AiChatService;
import com.ynp.agent.service.BaseService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AI 聊天服务实现
 *
 * <p>提供流式和同步两种 AI 对话模式</p>
 *
 * @author ynp
 */
@Slf4j
@Service
public class AiChatServiceImpl extends BaseService implements AiChatService {

    /**
     * 默认模型编码
     */
    private static final String DEFAULT_MODEL_CODE = "gpt-4o";

    /**
     * AI 助手发送者ID
     */
    private static final Long AI_SENDER_ID = -1L;

    /**
     * 默认系统提示词
     */
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个有帮助的AI助手，请用中文回答问题。";

    /**
     * 语义搜索返回的相关记忆数量
     */
    private static final int RELEVANT_MEMORIES_TOP_K = 5;

    /**
     * 流式聊天
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param content 用户消息内容
     * @param modelCode 模型编码（可选）
     * @param systemPrompt 系统提示词（可选）
     * @return 流式事件流
     */
    @Override
    public Flux<StreamChatEvent> streamChat(Long userId, Long conversationId, String content,
                                             String modelCode, String systemPrompt) {

        // 1. 创建 Sink 用于发送流式事件
        Sinks.Many<StreamChatEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 2. 确定使用的模型
        String finalModelCode = determineModelCode(conversationId, modelCode);

        // 3. 获取流式模型
        StreamingChatModel streamingModel = modelSelectorService.getStreamingChatModel(finalModelCode);
        if (Objects.isNull(streamingModel)) {
            log.error("无法获取流式模型: {}", finalModelCode);
            sink.tryEmitNext(StreamChatEvent.error("模型不可用: " + finalModelCode));
            sink.tryEmitComplete();
            return sink.asFlux();
        }

        // 4. 异步执行流式对话
        executeStreamChat(sink, streamingModel, userId, conversationId, content, finalModelCode, systemPrompt);

        return sink.asFlux();
    }

    /**
     * 同步聊天
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param content 用户消息内容
     * @param modelCode 模型编码（可选）
     * @return AI 响应内容
     */
    @Override
    public String chat(Long userId, Long conversationId, String content, String modelCode) {
        // 1. 确定使用的模型
        String finalModelCode = determineModelCode(conversationId, modelCode);

        // 2. 获取同步模型
        ChatModel chatModel = modelSelectorService.getChatModel(finalModelCode);
        if (Objects.isNull(chatModel)) {
            log.error("无法获取聊天模型: {}", finalModelCode);
            throw new RuntimeException("模型不可用: " + finalModelCode);
        }

        // 3. 保存用户消息
        Message userMessage = saveUserMessage(userId, conversationId, content);

        // 4. 构建消息列表
        List<dev.langchain4j.data.message.ChatMessage> messages = buildChatMessages(
                conversationId, DEFAULT_SYSTEM_PROMPT, content);

        // 5. 调用模型
        ChatResponse response = chatModel.chat(messages);
        String aiReply = response.aiMessage().text();

        // 6. 保存 AI 回复
        saveAiMessage(conversationId, aiReply, finalModelCode, null);

        // 7. 更新会话最后消息
        updateConversationLastMessage(conversationId);

        return aiReply;
    }

    /**
     * 执行流式聊天
     *
     * @param sink 事件发送器
     * @param streamingModel 流式模型
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param content 用户消息内容
     * @param modelCode 模型编码
     * @param systemPrompt 系统提示词
     */
    private void executeStreamChat(Sinks.Many<StreamChatEvent> sink,
                                   StreamingChatModel streamingModel,
                                   Long userId, Long conversationId, String content,
                                   String modelCode, String systemPrompt) {

        try {
            // 1. 保存用户消息
            Message userMessage = saveUserMessage(userId, conversationId, content);

            // 2. 确定系统提示词
            String finalSystemPrompt = Objects.nonNull(systemPrompt) && !systemPrompt.isEmpty()
                    ? systemPrompt : DEFAULT_SYSTEM_PROMPT;

            // 3. 构建消息列表
            List<dev.langchain4j.data.message.ChatMessage> messages = buildChatMessages(
                    conversationId, finalSystemPrompt, content);

            // 4. 用于收集完整响应
            StringBuilder fullResponse = new StringBuilder();

            // 5. 调用流式模型
            streamingModel.chat(messages, new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    // 发送内容片段
                    fullResponse.append(partialResponse);
                    sink.tryEmitNext(StreamChatEvent.chunk(partialResponse));
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    // 计算 Token 数量
                    Integer tokenCount = null;
                    if (Objects.nonNull(response.tokenUsage())) {
                        tokenCount = response.tokenUsage().totalTokenCount();
                    }

                    // 保存 AI 回复到数据库
                    Message aiMessage = saveAiMessage(conversationId, fullResponse.toString(),
                            modelCode, tokenCount);

                    // 更新会话最后消息
                    updateConversationLastMessage(conversationId);

                    // 发送完成事件
                    sink.tryEmitNext(StreamChatEvent.done(aiMessage.getId(), conversationId, tokenCount));
                    sink.tryEmitComplete();

                    log.info("流式对话完成，会话ID: {}, 响应长度: {}", conversationId, fullResponse.length());
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式对话错误，会话ID: {}", conversationId, error);
                    sink.tryEmitNext(StreamChatEvent.error(error.getMessage()));
                    sink.tryEmitComplete();
                }
            });

        } catch (Exception e) {
            log.error("执行流式对话异常，会话ID: {}", conversationId, e);
            sink.tryEmitNext(StreamChatEvent.error("对话执行异常: " + e.getMessage()));
            sink.tryEmitComplete();
        }
    }

    /**
     * 确定使用的模型编码
     *
     * @param conversationId 会话ID
     * @param requestModelCode 请求中指定的模型编码
     * @return 最终使用的模型编码
     */
    private String determineModelCode(Long conversationId, String requestModelCode) {
        // 1. 优先使用请求中指定的模型
        if (Objects.nonNull(requestModelCode) && !requestModelCode.isEmpty()) {
            return requestModelCode;
        }

        // 2. 尝试从会话获取模型配置
        Conversation conversation = conversationManager.selectById(conversationId);
        if (Objects.nonNull(conversation) && Objects.nonNull(conversation.getModelCode())) {
            return conversation.getModelCode();
        }

        // 3. 使用默认模型
        return DEFAULT_MODEL_CODE;
    }

    /**
     * 构建聊天消息列表
     *
     * <p>包含系统提示词、相关历史记忆、Redis 缓存的近期消息和当前用户消息</p>
     *
     * @param conversationId 会话ID
     * @param systemPrompt 系统提示词
     * @param userContent 用户消息内容
     * @return LangChain4j 消息列表
     */
    private List<dev.langchain4j.data.message.ChatMessage> buildChatMessages(
            Long conversationId, String systemPrompt, String userContent) {

        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();

        // 1. 构建增强的系统提示词（包含相关记忆）
        String enhancedSystemPrompt = buildEnhancedSystemPrompt(conversationId, systemPrompt, userContent);
        messages.add(SystemMessage.from(enhancedSystemPrompt));

        // 2. 从 Redis 加载历史消息
        List<dev.langchain4j.data.message.ChatMessage> historyMessages =
                redisChatMemoryStore.getMessages(conversationId.toString());
        messages.addAll(historyMessages);

        // 3. 添加当前用户消息
        messages.add(UserMessage.from(userContent));

        return messages;
    }

    /**
     * 构建增强的系统提示词
     *
     * <p>通过语义搜索获取相关历史记忆，增强 AI 的上下文理解能力</p>
     *
     * @param conversationId 会话ID
     * @param baseSystemPrompt 基础系统提示词
     * @param userContent 当前用户消息
     * @return 增强后的系统提示词
     */
    private String buildEnhancedSystemPrompt(Long conversationId, String baseSystemPrompt, String userContent) {
        try {
            // 通过语义搜索获取相关历史记忆
            List<String> relevantMemories = embeddingService.searchRelevantMemories(
                    conversationId, userContent, RELEVANT_MEMORIES_TOP_K);

            // 如果没有相关记忆，返回原始提示词
            if (Objects.isNull(relevantMemories) || relevantMemories.isEmpty()) {
                return baseSystemPrompt;
            }

            // 构建增强提示词
            StringBuilder enhanced = new StringBuilder(baseSystemPrompt);
            enhanced.append("\n\n以下是与当前话题相关的历史对话记忆，请在回答时参考这些上下文：\n");
            for (int i = 0; i < relevantMemories.size(); i++) {
                enhanced.append(i + 1).append(". ").append(relevantMemories.get(i)).append("\n");
            }

            return enhanced.toString();

        } catch (Exception e) {
            // 语义搜索失败不影响主流程
            log.warn("语义搜索相关记忆失败，conversationId: {}, 错误: {}", conversationId, e.getMessage());
            return baseSystemPrompt;
        }
    }

    /**
     * 保存用户消息到数据库
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param content 消息内容
     * @return 保存后的消息实体
     */
    private Message saveUserMessage(Long userId, Long conversationId, String content) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(userId)
                .role("user")
                .content(content)
                .contentType("TEXT")
                .status(1)
                .sendTime(LocalDateTime.now())
                .build();

        messageManager.insertMessage(message);

        // 更新 Redis 缓存
        updateChatMemory(conversationId, "user", content);

        // 创建向量化任务（异步处理）
        createEmbeddingTask(message.getId(), conversationId, userId, content);

        return message;
    }

    /**
     * 保存 AI 回复到数据库
     *
     * @param conversationId 会话ID
     * @param content 回复内容
     * @param modelCode 模型编码
     * @param tokenCount Token 数量
     * @return 保存后的消息实体
     */
    private Message saveAiMessage(Long conversationId, String content, String modelCode, Integer tokenCount) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(AI_SENDER_ID)
                .role("assistant")
                .content(content)
                .contentType("TEXT")
                .modelCode(modelCode)
                .tokenCount(tokenCount)
                .status(1)
                .sendTime(LocalDateTime.now())
                .build();

        messageManager.insertMessage(message);

        // 更新 Redis 缓存
        updateChatMemory(conversationId, "assistant", content);

        // 获取会话所属用户，创建向量化任务
        Conversation conversation = conversationManager.selectById(conversationId);
        if (Objects.nonNull(conversation) && Objects.nonNull(conversation.getUserId())) {
            createEmbeddingTask(message.getId(), conversationId, conversation.getUserId(), content);
        }

        return message;
    }

    /**
     * 更新聊天记忆缓存
     *
     * @param conversationId 会话ID
     * @param role 角色
     * @param content 内容
     */
    private void updateChatMemory(Long conversationId, String role, String content) {
        String memoryId = conversationId.toString();

        // 获取现有消息
        List<dev.langchain4j.data.message.ChatMessage> messages =
                new ArrayList<>(redisChatMemoryStore.getMessages(memoryId));

        // 添加新消息
        if ("user".equals(role)) {
            messages.add(UserMessage.from(content));
        } else if ("assistant".equals(role)) {
            messages.add(AiMessage.from(content));
        }

        // 保持最近10条消息
        if (messages.size() > 10) {
            messages = messages.subList(messages.size() - 10, messages.size());
        }

        // 更新缓存
        redisChatMemoryStore.updateMessages(memoryId, messages);
    }

    /**
     * 更新会话最后消息信息
     *
     * @param conversationId 会话ID
     */
    private void updateConversationLastMessage(Long conversationId) {
        // 获取最后一条消息
        Message lastMessage = messageManager.selectLastMessage(conversationId);
        if (Objects.nonNull(lastMessage)) {
            conversationManager.updateLastMessage(conversationId, lastMessage.getId(), lastMessage.getSendTime());
        }
    }

    /**
     * 创建向量化任务
     *
     * <p>异步将消息内容向量化存储，用于长期记忆语义搜索</p>
     *
     * @param messageId 消息ID
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param content 消息内容
     */
    private void createEmbeddingTask(Long messageId, Long conversationId, Long userId, String content) {
        try {
            embeddingService.createEmbeddingTask(messageId, conversationId, userId, content);
        } catch (Exception e) {
            // 向量化失败不影响主流程，仅记录日志
            log.warn("创建向量化任务失败，messageId: {}, 错误: {}", messageId, e.getMessage());
        }
    }
}
