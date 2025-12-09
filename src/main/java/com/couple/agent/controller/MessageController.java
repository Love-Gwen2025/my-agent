package com.couple.agent.controller;


import com.couple.agent.model.dto.Result;
import com.couple.agent.model.vo.ChatReplyVo;
import com.couple.agent.model.param.MessageSendParam;
import com.couple.agent.model.vo.MessageVo;
import com.couple.agent.utils.SessionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Result<ChatReplyVo> chat(@Valid @RequestBody MessageSendParam param) {
        ChatReplyVo reply = chatApiService.chat(SessionUtil.get().getId(), param.getConversationId(), param.getContent());
        return Result.ok(reply);
    }
}
