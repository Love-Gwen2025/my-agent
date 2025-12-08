package com.ynp.agent.controller;


import com.ynp.agent.model.dto.Result;
import com.ynp.agent.model.param.MessageSendParam;
import com.ynp.agent.model.vo.MessageVo;
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
}
