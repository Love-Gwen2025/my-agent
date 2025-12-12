package com.couple.agent.service;

import com.couple.agent.model.vo.StreamChatEvent;
import reactor.core.publisher.Flux;

/**
 * AI 聊天服务接口
 *
 * <p>提供 AI 对话能力，支持同步和流式两种模式</p>
 *
 * @author ynp
 */
public interface AiChatService {

    /**
     * 流式聊天
     * 以 SSE 方式实时返回 AI 响应内容
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param parentId 分支的父消息ID
     * @param content 用户消息内容
     * @param modelCode 模型编码（可选）
     * @param systemPrompt 系统提示词（可选）
     * @return 流式事件流
     */
    Flux<StreamChatEvent> streamChat(Long userId, Long conversationId, Long parentId, String content,
                                      String modelCode, String systemPrompt);

    /**
     * 同步聊天
     *阻塞等待 AI 完整响应
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param parentId 分支的父消息ID
     * @param content 用户消息内容
     * @param modelCode 模型编码（可选）
     * @return AI 响应内容
     */
    String chat(Long userId, Long conversationId, Long parentId, String content, String modelCode);
}
