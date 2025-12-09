package com.couple.agent.service;

import com.couple.agent.exception.BizErrorCode;
import com.couple.agent.exception.BizException;
import com.couple.agent.model.domain.Conversation;
import com.couple.agent.model.domain.Message;
import com.couple.agent.model.domain.User;
import com.couple.agent.model.vo.ChatReplyVo;
import com.couple.agent.model.dto.api.ConversationView;
import com.couple.agent.model.dto.api.HistoryMessageView;
import com.couple.agent.model.dto.api.SessionView;
import com.couple.agent.model.param.MessageSendParam;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 面向 /api/* 的会话与聊天编排服务。
 */
@Service
public class ChatApiService extends BaseService {



    /**
     * 创建机器人会话：默认 type=3，成员包含当前用户。
     */
    public ConversationView createConversation(Long userId, String title) {
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .title(StringUtils.hasText(title) ? title : "与聊天助手的会话")
                .build();
        Long conversationId = conversationManager.insertConversation(conversation);
        LocalDateTime now = LocalDateTime.now();
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        return toConversationView(conversation);
    }


    public List<ConversationView> listConversations(Long userId) {
        if (Objects.isNull(userId)) {
            return Collections.emptyList();
        }
        List<Conversation> conversations = conversationManager.listByUser(userId);
        return conversations.stream()
                .map(this::toConversationView)
                .collect(Collectors.toList());
    }

    /**
     * 1. 更新会话标题，要求成员权限。
     */
    public ConversationView modifyConversation(Long userId, Long conversationId, String title) {
        ensureOwnership(userId, conversationId);
        int affected = conversationManager.updateTitle(conversationId, title);
        if (affected <= 0) {
            throw new BizException(BizErrorCode.MESSAGE_CONVERSATION_NOT_FOUND, "会话不存在或已删除");
        }
        Conversation conversation = conversationManager.selectById(conversationId);
        return toConversationView(conversation);
    }

    /**
     * 1. 删除会话及消息，要求成员权限。
     */
    public void deleteConversation(Long userId, Long conversationId) {
        ensureOwnership(userId, conversationId);
        messageManager.deleteByConversation(conversationId);
        conversationManager.deleteConversation(conversationId);
    }

    /**
     * 1. 查询会话历史，确保用户有权限。
     */
    public List<HistoryMessageView> history(Long userId, Long conversationId) {
        ensureOwnership(userId, conversationId);
        List<Message> messages = messageManager.listByConversation(conversationId);
        if (CollectionUtils.isEmpty(messages)) {
            return Collections.emptyList();
        }
        return messages.stream()
                .map(message -> toHistoryView(message, userId))
                .collect(Collectors.toList());
    }

    /**
     * 1. 查询单个会话视图，不修改数据。
     */
    public ConversationView getConversation(Long userId, Long conversationId) {
        ensureOwnership(userId, conversationId);
        Conversation conversation = conversationManager.selectById(conversationId);
        return toConversationView(conversation);
    }

    /**
     * 1. 发送用户消息，调用 GPT 获取回复，统一入库。
     */
    public ChatReplyVo chat(Long userId, Long conversationId, String content) {
        ensureOwnership(userId, conversationId);
        // 2. 用户消息入库
        MessageSendParam sendParam = MessageSendParam.builder()
                .conversationId(conversationId)
                .content(content)
                .contentType("TEXT")
                .build();
        Message userMessage = persistUserMessage(userId, sendParam);
        // 3. 调用助手并入库回复
        String reply = assistant.chat(conversationId.intValue(), content);
        Message assistantMessage = persistAssistantMessage(conversationId, reply);
        conversationManager.updateLastMessage(conversationId, assistantMessage.getId(), assistantMessage.getSendTime());
        return ChatReplyVo.builder().reply(reply).build();
    }

    /**
     * 1. 退出登录：删除 Redis 中的会话索引与数据。
     */
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        String userId = null;
        try {
            var claims = jwtUtil.parseToken(token);
            if (Objects.nonNull(claims)) {
                userId = String.valueOf(claims.get("userId"));
            }
        } catch (Exception ex) {
            return;
        }
        if (!StringUtils.hasText(userId)) {
            return;
        }
        String sessionKey = com.couple.agent.model.domain.CurrentSession.sessionKey(userId, token);
        String indexKey = com.couple.agent.model.domain.CurrentSession.indexKey(userId);
        redisTemplate.opsForValue().getOperations().delete(sessionKey);
        redisTemplate.opsForZSet().remove(indexKey, sessionKey);
    }

    /**
     * 1. 根据主键获取用户信息。
     */
    public User loadUserById(Long userId) {
        if (Objects.isNull(userId)) {
            return null;
        }
        return userManager.selectById(userId);
    }

    /**
     * 1. 根据用户编码获取用户信息。
     */
    public User loadUserByCode(String userCode) {
        if (!StringUtils.hasText(userCode)) {
            return null;
        }
        return userManager.selectByUserCode(userCode);
    }

    private SessionView.UserView buildUserView(User user) {
        if (Objects.isNull(user)) {
            return null;
        }
        return SessionView.UserView.builder()
                .username(user.getUserCode())
                .displayName(StringUtils.hasText(user.getUserName()) ? user.getUserName() : user.getUserCode())
                .build();
    }

    private ConversationView toConversationView(Conversation conversation) {
        if (Objects.isNull(conversation)) {
            return null;
        }
        LocalDateTime updatedAt = Objects.nonNull(conversation.getLastMessageAt())
                ? conversation.getLastMessageAt()
                : conversation.getUpdateTime();
        return ConversationView.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreateTime())
                .updatedAt(updatedAt)
                .build();
    }

    private HistoryMessageView toHistoryView(Message message, Long currentUserId) {
        String role = Objects.equals(message.getSenderId(), currentUserId) ? "user" : "assistant";
        return HistoryMessageView.builder()
                .role(role)
                .content(message.getContent())
                .createdAt(message.getSendTime())
                .attachments(Collections.emptyList())
                .build();
    }

    private void ensureOwnership(Long userId, Long conversationId) {
        Conversation conversation = conversationManager.selectById(conversationId);
        if (Objects.isNull(conversation)) {
            throw new BizException(BizErrorCode.MESSAGE_CONVERSATION_NOT_FOUND, "会话不存在或已被删除");
        }
        if (!Objects.equals(conversation.getUserId(), userId)) {
            throw new BizException(BizErrorCode.MESSAGE_FORBIDDEN, "您不在该会话中，无法操作");
        }
    }

    private Message persistUserMessage(Long userId, MessageSendParam param) {
        LocalDateTime now = LocalDateTime.now();
        Message message = Message.builder()
                .conversationId(param.getConversationId())
                .senderId(userId)
                .content(param.getContent())
                .contentType(StringUtils.hasText(param.getContentType()) ? param.getContentType() : "TEXT")
                .status(1)
                .sendTime(now)
                .build();
        messageManager.insertMessage(message);
        return message;
    }

    private Message persistAssistantMessage(Long conversationId, String reply) {
        LocalDateTime now = LocalDateTime.now();
        Message assistantMessage = Message.builder()
                .conversationId(conversationId)
                .senderId(-1L)
                .content(reply)
                .contentType("TEXT")
                .status(1)
                .sendTime(now)
                .build();
        messageManager.insertMessage(assistantMessage);
        return assistantMessage;
    }
}
