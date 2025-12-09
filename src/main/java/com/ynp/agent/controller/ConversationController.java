package com.ynp.agent.controller;

import com.ynp.agent.exception.BizErrorCode;
import com.ynp.agent.exception.BizException;
import com.ynp.agent.model.domain.CurrentSession;
import com.ynp.agent.model.dto.Result;
import com.ynp.agent.model.dto.api.ConversationView;
import com.ynp.agent.model.dto.api.HistoryMessageView;
import com.ynp.agent.model.param.ConversationParam;
import com.ynp.agent.utils.SessionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/conversation")
@Tag(name = "会话管理")
public class ConversationController extends BaseController{
    @PostMapping(value = "/create")
    @Operation(description = "会话创建")
    public Result<Long> create(@Validated @RequestBody ConversationParam inParam){
        return Result.ok(conversationService.create(inParam));
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前用户的会话列表")
    public Result<List<ConversationView>> list() {
        return Result.ok(chatApiService.listConversations(SessionUtil.get().getId()));
    }

    @PostMapping("/create/assistant")
    @Operation(summary = "创建机器人会话")
    public Result<ConversationView> createAssistant(@RequestBody(required = false) ConversationParam conversationParam) {
        return Result.ok(chatApiService.createConversation(SessionUtil.get().getId(), conversationParam.getTitle()));
    }

    @PatchMapping("/modify")
    @Operation(summary = "修改会话")
    public Result<ConversationView> modifyConversation(@RequestBody ConversationParam conversationParam) {
        return Result.ok(chatApiService.modifyConversation(SessionUtil.get().getId(),conversationParam.getId(), conversationParam.getTitle()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话")
    public Result<Void> delete(@PathVariable("id") Long conversationId) {
        chatApiService.deleteConversation(SessionUtil.get().getId(), conversationId);
        return Result.ok();
    }

    @GetMapping("/history")
    @Operation(summary = "查询会话历史")
    public Result<List<HistoryMessageView>> history(@RequestParam("conversationId") Long conversationId) {
        return Result.ok(chatApiService.history(SessionUtil.get().getId(), conversationId));
    }
}
