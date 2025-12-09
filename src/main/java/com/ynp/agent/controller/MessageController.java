package com.ynp.agent.controller;


import com.ynp.agent.model.dto.Result;
import com.ynp.agent.model.dto.api.ChatReplyView;
import com.ynp.agent.model.param.MessageSendParam;
import com.ynp.agent.model.vo.MessageVo;
import com.ynp.agent.utils.SessionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 消息接口：提供 REST 方式的消息发送能力。
 */
@RestController
@RequestMapping("/message")
@Tag(name = "消息管理")
public class MessageController extends BaseController {

    @PostMapping("/send")
    @Operation(summary = "发送消息")
    public Result<MessageVo> send(@Valid @RequestBody MessageSendParam param) {
        return Result.ok(messageService.send(param));
    }

    @PostMapping("/chat")
    @Operation(summary = "AI 对话")
    public Result<ChatReplyView> chat(@Valid @RequestBody MessageSendParam param) {
        /*1. 校验登录态，确保调用方已登录。*/
        if (Objects.isNull(SessionUtil.get())) {
            return Result.error("AUTH-401", "用户未登录或会话已过期",new ChatReplyView());
        }
        /*2. 调用聊天编排服务，发送用户消息并获取助手回复。*/
        ChatReplyView reply = chatApiService.chat(SessionUtil.get().getId(), param.getConversationId(), param.getContent());
        return Result.ok(reply);
    }
}
