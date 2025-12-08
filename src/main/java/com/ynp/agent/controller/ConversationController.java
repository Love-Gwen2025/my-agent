package com.ynp.agent.controller;

import com.ynp.agent.model.dto.Result;
import com.ynp.agent.model.param.ConversationParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversation")
@Tag(name = "会话管理")
public class ConversationController extends BaseController{
    @PostMapping(value = "/create")
    @Operation(description = "会话创建")
    public Result<Long> create(@Validated @RequestBody ConversationParam inParam){
        return Result.ok(conversationService.create(inParam));
    }
}
