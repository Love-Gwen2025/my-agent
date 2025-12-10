package com.couple.agent.controller;

import com.couple.agent.model.dto.Result;
import com.couple.agent.model.vo.ConversationVo;
import com.couple.agent.model.vo.HistoryMessageVo;
import com.couple.agent.model.param.ConversationParam;
import com.couple.agent.utils.SessionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.Objects;

@RestController
@RequestMapping("/conversation")
@Tag(name = "会话管理")
public class ConversationController extends BaseController{

    @GetMapping("/list")
    @Operation(summary = "查询当前用户的会话列表")
    public Result<List<ConversationVo>> list() {
        return Result.ok(conversationService.listConversations(SessionUtil.get().getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询指定会话信息")
    public Result<ConversationVo> get(@PathVariable(value = "id") Long id) {
        return Result.ok(conversationService.getConversation(id));
    }

    @PostMapping("/create/assistant")
    @Operation(summary = "创建机器人会话")
    public Result<Long> createAssistant(@RequestBody ConversationParam conversationParam) {
        String title = Objects.nonNull(conversationParam) ? conversationParam.getTitle() : null;
        return Result.ok(conversationService.createConversation(title));
    }

    @PatchMapping("/modify")
    @Operation(summary = "修改会话")
    public Result<Void> modifyConversation(@RequestBody ConversationParam conversationParam) {
        conversationService.modifyConversation(conversationParam);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话")
    public Result<Void> delete(@PathVariable("id") Long conversationId) {
        conversationService.deleteConversation(conversationId);
        return Result.ok();
    }

    @GetMapping("/history")
    @Operation(summary = "查询会话历史")
    public Result<List<HistoryMessageVo>> history(@RequestParam("conversationId") Long conversationId) {
        return Result.ok(conversationService.history(conversationId));
    }
}
