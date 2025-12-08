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
import java.util.Map;

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
        /*1. 拉取线程上下文的会话信息，未登录直接抛出业务异常，避免越权访问。*/
        CurrentSession session = SessionUtil.get();
        if (session == null) {
            throw new BizException(BizErrorCode.AUTH_UNAUTHORIZED, "用户未登录或会话已过期");
        }
        /*2. 调用聊天编排服务查询会话列表并包装返回。*/
        return Result.ok(chatApiService.listConversations(session.getId()));
    }

    @PostMapping("/create/assistant")
    @Operation(summary = "创建机器人会话")
    public Result<ConversationView> createAssistant(@RequestBody(required = false) Map<String, Object> body) {
        /*1. 校验登录态，确保只有登录用户才能创建机器人会话。*/
        CurrentSession session = SessionUtil.get();
        if (session == null) {
            throw new BizException(BizErrorCode.AUTH_UNAUTHORIZED, "用户未登录或会话已过期");
        }
        /*2. 解析会话标题并创建默认机器人会话。*/
        String title = body != null && body.get("title") != null ? body.get("title").toString() : "";
        return Result.ok(chatApiService.createConversation(session.getId(), title));
    }

    @PatchMapping("/{id}/title")
    @Operation(summary = "重命名会话")
    public Result<ConversationView> rename(@PathVariable("id") Long conversationId,
                                           @RequestBody Map<String, Object> body) {
        /*1. 校验登录态，避免未认证用户修改会话信息。*/
        CurrentSession session = SessionUtil.get();
        if (session == null) {
            throw new BizException(BizErrorCode.AUTH_UNAUTHORIZED, "用户未登录或会话已过期");
        }
        /*2. 读取新标题并调用服务完成重命名。*/
        String title = body != null && body.get("title") != null ? body.get("title").toString() : "";
        return Result.ok(chatApiService.renameConversation(session.getId(), conversationId, title));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话")
    public Result<Void> delete(@PathVariable("id") Long conversationId) {
        /*1. 校验登录态，避免未认证用户删除会话。*/
        CurrentSession session = SessionUtil.get();
        if (session == null) {
            throw new BizException(BizErrorCode.AUTH_UNAUTHORIZED, "用户未登录或会话已过期");
        }
        /*2. 调用服务删除会话及其关联消息/成员关系。*/
        chatApiService.deleteConversation(session.getId(), conversationId);
        return Result.ok();
    }

    @GetMapping("/history")
    @Operation(summary = "查询会话历史")
    public Result<List<HistoryMessageView>> history(@RequestParam("conversationId") Long conversationId) {
        /*1. 校验登录态，未登录返回 401。*/
        CurrentSession session = SessionUtil.get();
        if (session == null) {
            throw new BizException(BizErrorCode.AUTH_UNAUTHORIZED, "用户未登录或会话已过期");
        }
        /*2. 拉取会话历史并返回给前端。*/
        return Result.ok(chatApiService.history(session.getId(), conversationId));
    }
}
