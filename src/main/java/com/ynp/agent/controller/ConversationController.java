package com.ynp.agent.controller;

import com.ynp.agent.dto.ChatRequest;
import com.ynp.agent.dto.ConversationTitleRequest;
import com.ynp.agent.helper.UserContextHolder;
import com.ynp.agent.helper.model.JwtPayload;
import com.ynp.agent.service.ChatService;
import com.ynp.agent.service.ConversationService;
import com.ynp.agent.vo.ChatReplyVO;
import com.ynp.agent.vo.ConversationListVO;
import com.ynp.agent.vo.ConversationWrapperVO;
import com.ynp.agent.vo.HistoryResponseVO;
import com.ynp.agent.vo.MessageVO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 会话与聊天相关接口。
 */
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatService chatService;

    public ConversationController(ConversationService conversationService, ChatService chatService) {
        this.conversationService = conversationService;
        this.chatService = chatService;
    }

    /**
     * 1. 获取当前用户会话列表。
     */
    @GetMapping
    public ResponseEntity<ConversationListVO> list() {
        JwtPayload payload = UserContextHolder.getContext();
        /* 1. 查询当前用户的会话列表 */
        ConversationListVO vo = conversationService.listConversations(payload.getUserId());
        return ResponseEntity.ok(vo);
    }

    /**
     * 1. 创建会话。
     */
    @PostMapping
    public ResponseEntity<ConversationWrapperVO> create(@RequestBody(required = false) ConversationTitleRequest request) {
        JwtPayload payload = UserContextHolder.getContext();
        String title = Objects.nonNull(request) ? request.getTitle() : "";
        /* 1. 创建会话 */
        ConversationWrapperVO vo = conversationService.createConversation(payload.getUserId(), title);
        return ResponseEntity.ok(vo);
    }

    /**
     * 1. 重命名会话。
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ConversationWrapperVO> rename(@PathVariable("id") Long id, @Valid @RequestBody ConversationTitleRequest request) {
        JwtPayload payload = UserContextHolder.getContext();
        /* 1. 重命名会话 */
        ConversationWrapperVO vo = conversationService.rename(payload.getUserId(), id, request.getTitle());
        return ResponseEntity.ok(vo);
    }

    /**
     * 1. 删除会话。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageVO> delete(@PathVariable("id") Long id) {
        JwtPayload payload = UserContextHolder.getContext();
        /* 1. 删除会话与附件 */
        conversationService.deleteConversation(payload.getUserId(), id);
        return ResponseEntity.ok(new MessageVO("会话已删除"));
    }

    /**
     * 1. 查询会话历史。
     */
    @GetMapping("/history")
    public ResponseEntity<HistoryResponseVO> history(@RequestParam(value = "conversationId") Long conversationId) {
        JwtPayload payload = UserContextHolder.getContext();
        /* 1. 查询历史消息 */
        HistoryResponseVO vo = chatService.fetchHistory(payload.getUserId(), conversationId);
        return ResponseEntity.ok(vo);
    }

    /**
     * 1. 发送消息并获取回复。
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatReplyVO> chat(@RequestParam("conversationId") Long conversationId,
                                            @RequestParam(value = "message", required = false) String message,
                                            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        JwtPayload payload = UserContextHolder.getContext();
        List<MultipartFile> safeFiles = Objects.isNull(files) ? Collections.emptyList() : files;
        /* 1. 发送消息并获取 AI 回复 */
        ChatReplyVO vo = chatService.chat(payload.getUserId(), conversationId, message, safeFiles);
        return ResponseEntity.ok(vo);
    }
}
