package com.couple.agent.service.impl;

import com.couple.agent.model.domain.Conversation;
import com.couple.agent.model.domain.Message;
import com.couple.agent.model.vo.StreamChatEvent;
import com.couple.agent.service.AiChatService;
import com.couple.agent.service.BaseService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * AI 聊天服务实现
 * 提供流式和同步两种 AI 对话模式
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
    public Flux<StreamChatEvent> streamChat(Long userId, Long conversationId, Long parentId, String content,
                                             String modelCode, String systemPrompt) {
        // 1. 创建单播 Sink 承载流式事件
        Sinks.Many<StreamChatEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 2. 计算分支基点并刷新聊天记忆，确保上下文仅包含该分支
        Long branchParentId = resolveParentMessageId(conversationId, parentId);
        List<Message> branchChain = loadBranchChain(conversationId, branchParentId);
        refreshChatMemory(conversationId, branchChain, systemPrompt);

        // 3. 确定模型并执行流式对话
        String finalModelCode = determineModelCode(conversationId, modelCode);
        executeStreamChat(sink, userId, conversationId, branchParentId, content, finalModelCode);

        // 4. 返回事件流
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
    public String chat(Long userId, Long conversationId, Long parentId, String content, String modelCode) {
        // 1. 确定使用的模型
        String finalModelCode = determineModelCode(conversationId, modelCode);
        // 2. 获取同步模型
        ChatModel chatModel = modelSelectorService.getChatModel(finalModelCode);
        if (Objects.isNull(chatModel)) {
            log.error("无法获取聊天模型: {}", finalModelCode);
            throw new RuntimeException("模型不可用: " + finalModelCode);
        }

        // 3. 计算分支基点，确保上下文沿选定分支构建
        Long branchParentId = resolveParentMessageId(conversationId, parentId);
        List<ChatMessage> messages = buildBranchChatMessages(conversationId, branchParentId, DEFAULT_SYSTEM_PROMPT, content);

        // 4. 保存用户消息（挂载到分支）
        Message userMessage = saveUserMessage(userId, conversationId, content, branchParentId);

        // 5. 调用模型
        ChatResponse response = chatModel.chat(messages);
        String aiReply = response.aiMessage().text();

        // 6. 保存 AI 回复并更新指针
        saveAiMessage(conversationId, aiReply, finalModelCode, null, userMessage.getId());
        updateConversationLastMessage(conversationId);

        return aiReply;
    }

    /**
     * 执行流式聊天
     *
     * @param sink 事件发送器
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param parentId 父消息ID
     * @param content 用户消息内容
     * @param modelCode 模型编码
     */
    private void executeStreamChat(Sinks.Many<StreamChatEvent> sink,
                                   Long userId, Long conversationId, Long parentId, String content, String modelCode) {

        try {
            // 1. 保存用户消息并写入缓存、向量任务
            Message userMessage = saveUserMessage(userId, conversationId, content, parentId);

            // 2. 用于收集完整响应内容
            StringBuilder fullResponse = new StringBuilder();

            // 3. 调用 AI 助手的流式接口
            TokenStream tokenStream = assistant.chat(conversationId, content);

            // 4. 监听分片回调，逐片推送事件
            tokenStream.onPartialResponse(partial -> {
                fullResponse.append(partial);
                sink.tryEmitNext(StreamChatEvent.chunk(partial));
            });

            // 5. 监听完成回调，落库并推送完成事件
            tokenStream.onCompleteResponse(chatResponse-> {
                Integer tokenCount = Objects.nonNull(chatResponse.tokenUsage()) ? chatResponse.tokenUsage().totalTokenCount() : null;
                Message aiMessage = saveAiMessage(conversationId, fullResponse.toString(), modelCode, tokenCount, userMessage.getId());
               //更新会话最新消息
                updateConversationLastMessage(conversationId);
                sink.tryEmitNext(StreamChatEvent.done(aiMessage.getId(), conversationId, tokenCount));
                sink.tryEmitComplete();
                log.info("流式对话完成，会话ID: {}, 响应长度: {}", conversationId, fullResponse.length());
            });

            // 6. 监听异常回调，推送错误事件
            tokenStream.onError(error -> {
                log.error("流式对话错误，会话ID: {}", conversationId, error);
                sink.tryEmitNext(StreamChatEvent.error(error.getMessage()));
                sink.tryEmitComplete();
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
        if (StringUtils.hasText(requestModelCode)) {
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
     * 构建分支上下文消息列表
     * 按 parent 链路堆叠历史，再拼接系统提示词与当前用户消息
     * @param conversationId 会话ID
     * @param branchParentId 分支父节点ID
     * @param systemPrompt 系统提示词
     * @param userContent 用户输入
     * @return 对话消息列表
     */
    private List<ChatMessage> buildBranchChatMessages(Long conversationId, Long branchParentId, String systemPrompt, String userContent) {
        List<Message> branchChain = loadBranchChain(conversationId, branchParentId);
        List<ChatMessage> messages = new ArrayList<>();

        // 1. 组装系统提示词（包含语义记忆）
        String enhancedSystemPrompt = buildEnhancedSystemPrompt(conversationId, systemPrompt, userContent);
        messages.add(SystemMessage.from(enhancedSystemPrompt));

        // 2. 拼接分支历史
        messages.addAll(convertMessagesToChatHistory(branchChain));

        // 3. 追加当前用户消息
        messages.add(UserMessage.from(userContent));
        return messages;
    }

    /**
     * 构建增强的系统提示词
     * 通过语义搜索获取相关历史记忆，增强 AI 的上下文理解能力
     * @param conversationId 会话ID
     * @param baseSystemPrompt 基础系统提示词
     * @param userContent 当前用户消息
     * @return 增强后的系统提示词
     */
    private String buildEnhancedSystemPrompt(Long conversationId, String baseSystemPrompt, String userContent) {
        try {
            // 1. 通过语义搜索获取相关历史记忆
            List<String> relevantMemories = embeddingService.searchRelevantMemories(
                    conversationId, userContent, RELEVANT_MEMORIES_TOP_K);

            // 如果没有相关记忆，返回原始提示词
            if (Objects.isNull(relevantMemories) || relevantMemories.isEmpty()) {
                return baseSystemPrompt;
            }

            // 2. 构建增强提示词
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
    private Message saveUserMessage(Long userId, Long conversationId, String content, Long parentId) {
        // 1. 构建并落库用户消息，关联分支父节点
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(userId)
                .role("user")
                .content(content)
                .parentId(parentId)
                .contentType("TEXT")
                .status(1)
                .build();

        messageManager.insertMessage(message);

        // 2. 创建向量化任务（异步处理）
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
     * @param parentId 父消息ID
     * @return 保存后的消息实体
     */
    private Message saveAiMessage(Long conversationId, String content, String modelCode, Integer tokenCount, Long parentId) {
        // 1. 构建并落库 AI 消息，确保父节点指向当前用户消息
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(AI_SENDER_ID)
                .role("assistant")
                .content(content)
                .parentId(parentId)
                .contentType("TEXT")
                .modelCode(modelCode)
                .tokenCount(tokenCount)
                .status(1)
                .build();

        messageManager.insertMessage(message);
        // 2. 获取会话所属用户，创建向量化任务
        Conversation conversation = conversationManager.selectById(conversationId);
        if (Objects.nonNull(conversation) && Objects.nonNull(conversation.getUserId())) {
            createEmbeddingTask(message.getId(), conversationId, conversation.getUserId(), content);
        }

        return message;
    }

    /**
     * 刷新聊天记忆缓存，确保仅包含当前分支
     *
     * @param conversationId 会话ID
     * @param branchChain 分支上的历史消息
     * @param systemPrompt 系统提示词
     */
    private void refreshChatMemory(Long conversationId, List<Message> branchChain, String systemPrompt) {
        // 1. 组装需要缓存的历史
        List<ChatMessage> history = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            history.add(SystemMessage.from(systemPrompt));
        }
        history.addAll(convertMessagesToChatHistory(branchChain));

        // 2. 无历史时清空，避免旧分支干扰
        if (CollectionUtils.isEmpty(history)) {
            redisChatMemoryStore.deleteMessages(conversationId);
            return;
        }

        // 3. 写入 Redis 作为流式上下文
        redisChatMemoryStore.updateMessages(conversationId, history);
    }

    /**
     * 计算分支父节点，优先使用入参，其次使用会话指针，最后回退到最新消息
     *
     * @param conversationId 会话ID
     * @param requestParentId 入参父节点
     * @return 实际使用的父节点
     */
    private Long resolveParentMessageId(Long conversationId, Long requestParentId) {
        // 1. 优先使用入参
        if (Objects.nonNull(requestParentId)) {
            return requestParentId;
        }

        // 2. 使用会话当前指针
        Conversation conversation = conversationManager.selectById(conversationId);
        if (Objects.nonNull(conversation) && Objects.nonNull(conversation.getCurrentMessageId())) {
            return conversation.getCurrentMessageId();
        }

        // 3. 回退为最近一条消息
        Message lastMessage = messageManager.selectLastMessage(conversationId);
        if (Objects.nonNull(lastMessage)) {
            return lastMessage.getId();
        }

        // 4. 无历史时返回空
        return null;
    }

    /**
     * 按 parent 链路加载分支历史（根到叶子顺序）
     *
     * @param conversationId 会话ID
     * @param branchParentId 分支父节点
     * @return 分支链路上的消息
     */
    private List<Message> loadBranchChain(Long conversationId, Long branchParentId) {
        // 1. 查询会话全部消息
        List<Message> messages = messageManager.listByConversation(conversationId);
        if (CollectionUtils.isEmpty(messages) || Objects.isNull(branchParentId)) {
            return Collections.emptyList();
        }

        // 2. 构建映射，便于向上回溯
        Map<Long, Message> messageMap = new HashMap<>();
        messages.forEach(item -> messageMap.put(item.getId(), item));

        // 3. 从分支叶子向上回溯到根
        List<Message> chain = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Long currentId = branchParentId;
        while (Objects.nonNull(currentId) && !Objects.equals(currentId, 0L)) {
            if (!visited.add(currentId)) {
                break;
            }
            Message current = messageMap.get(currentId);
            if (Objects.isNull(current)) {
                break;
            }
            chain.add(current);
            currentId = current.getParentId();
        }

        // 4. 反转为根到叶子的顺序
        Collections.reverse(chain);
        return chain;
    }

    /**
     * 将分支消息转换为 LangChain4j ChatMessage 历史
     *
     * @param branchChain 分支历史
     * @return ChatMessage 列表
     */
    private List<ChatMessage> convertMessagesToChatHistory(List<Message> branchChain) {
        // 1. 无历史直接返回空列表
        if (CollectionUtils.isEmpty(branchChain)) {
            return Collections.emptyList();
        }
        // 2. 按时间顺序转换为 ChatMessage
        List<ChatMessage> history = new ArrayList<>();
        branchChain.stream()
                .sorted(Comparator.comparing(Message::getCreateTime).thenComparing(Message::getId))
                .forEach(item -> {
                    if (!StringUtils.hasText(item.getContent())) {
                        return;
                    }
                    String role = item.getRole();
                    if (Objects.equals(role, "user")) {
                        history.add(UserMessage.from(item.getContent()));
                        return;
                    }
                    if (Objects.equals(role, "assistant")) {
                        history.add(AiMessage.from(item.getContent()));
                        return;
                    }
                    if (Objects.equals(role, "system")) {
                        history.add(SystemMessage.from(item.getContent()));
                    }
                });
        return history;
    }

    /**
     * 更新会话最后消息信息
     *
     * @param conversationId 会话ID
     */
    private void updateConversationLastMessage(Long conversationId) {
        // 1. 获取最后一条消息
        Message lastMessage = messageManager.selectLastMessage(conversationId);
        if (Objects.nonNull(lastMessage)) {
            conversationManager.updateLastMessage(conversationId, lastMessage.getId(), lastMessage.getCreateTime());
        }
    }

    /**
     * 创建向量化任务
     * 异步将消息内容向量化存储，用于长期记忆语义搜索
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
