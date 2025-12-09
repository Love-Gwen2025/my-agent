package com.couple.agent.controller;

import com.couple.agent.model.dto.Result;
import com.couple.agent.model.dto.api.StreamChatEvent;
import com.couple.agent.model.dto.api.StreamChatRequest;
import com.couple.agent.utils.SessionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.UUID;

/**
 * 流式聊天控制器
 *
 * <p>提供 SSE (Server-Sent Events) 流式聊天接口</p>
 *
 * @author ynp
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@Tag(name = "流式聊天")
public class StreamChatController extends BaseController {

    /**
     * 流式对话接口
     * 使用 SSE 实时推送 AI 响应内容<
     * @param request 聊天请求
     * @return SSE 事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "以 SSE 方式实时返回 AI 响应")
    public Flux<ServerSentEvent<StreamChatEvent>> streamChat(
            @Valid @RequestBody StreamChatRequest request) {
        Long userId = SessionUtil.get().getId();
        Long conversationId = request.getConversationId();
        String content = request.getContent();
        String modelCode = request.getModelCode();
        String systemPrompt = request.getSystemPrompt();

        log.info("开始流式对话，用户: {}, 会话: {}, 模型: {}", userId, conversationId, modelCode);

        // 调用 AI 聊天服务获取流式响应
        return aiChatService.streamChat(userId, conversationId, content, modelCode, systemPrompt)
                .map(this::createEvent)
                .onErrorResume(error -> {
                    log.error("流式对话发生错误", error);
                    return Flux.just(createErrorEvent(error.getMessage()));
                });
    }

    /**
     * 同步对话接口
     *
     * <p>等待 AI 完整响应后返回</p>
     *
     * @param request 聊天请求
     * @return AI 响应结果
     */
    @PostMapping("/send")
    @Operation(summary = "同步对话", description = "等待完整响应后返回")
    public Result<String> chat(@Valid @RequestBody StreamChatRequest request) {

        // 获取当前用户
        if (Objects.isNull(SessionUtil.get())) {
            return Result.error("AUTH-401", "用户未登录或会话已过期","错误");
        }

        Long userId = SessionUtil.get().getId();
        Long conversationId = request.getConversationId();
        String content = request.getContent();
        String modelCode = request.getModelCode();

        log.info("同步对话，用户: {}, 会话: {}", userId, conversationId);

        try {
            String reply = aiChatService.chat(userId, conversationId, content, modelCode);
            return Result.ok(reply);
        } catch (Exception e) {
            log.error("同步对话失败", e);
            return Result.error("CHAT-500", "对话失败: " + e.getMessage(),"错误");
        }
    }

    /**
     * 健康检查接口
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<String> health() {
        return Result.ok("Chat service is healthy");
    }

    /**
     * 创建 SSE 事件
     *
     * @param event 聊天事件
     * @return SSE 封装的事件
     */
    private ServerSentEvent<StreamChatEvent> createEvent(StreamChatEvent event) {
        return ServerSentEvent.<StreamChatEvent>builder()
                .id(UUID.randomUUID().toString())
                .event(event.getType())
                .data(event)
                .build();
    }

    /**
     * 创建错误事件
     *
     * @param errorMessage 错误信息
     * @return SSE 封装的错误事件
     */
    private ServerSentEvent<StreamChatEvent> createErrorEvent(String errorMessage) {
        StreamChatEvent errorEvent = StreamChatEvent.error(errorMessage);
        return ServerSentEvent.<StreamChatEvent>builder()
                .id(UUID.randomUUID().toString())
                .event("error")
                .data(errorEvent)
                .build();
    }
}
