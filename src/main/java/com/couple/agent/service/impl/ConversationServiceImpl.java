package com.couple.agent.service.impl;


import com.couple.agent.exception.BizErrorCode;
import com.couple.agent.exception.BizException;
import com.couple.agent.model.domain.Conversation;
import com.couple.agent.model.domain.Message;
import com.couple.agent.model.domain.User;
import com.couple.agent.model.param.ConversationParam;
import com.couple.agent.model.param.MessageSendParam;
import com.couple.agent.model.vo.ConversationVo;
import com.couple.agent.model.vo.ConversationHistoryVo;
import com.couple.agent.model.vo.MessageVo;
import com.couple.agent.model.vo.SessionVo;
import com.couple.agent.service.BaseService;
import com.couple.agent.service.ConversationService;
import com.couple.agent.utils.SessionUtil;
import com.couple.agent.utils.TreeBuildUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl extends BaseService implements ConversationService {

    /**
     * 创建机器人会话
     */
    @Override
    public Long createConversation(String title) {
        Conversation conversation = Conversation.builder()
                .userId(SessionUtil.get().getId())
                .title(StringUtils.hasText(title) ? title : "与聊天助手的会话")
                .build();
        return conversationManager.insertConversation(conversation);
    }

    @Override
    public List<ConversationVo> listConversations(Long userId) {
        if (Objects.isNull(userId)) {
            return Collections.emptyList();
        }
        List<Conversation> conversations = conversationManager.listByUser(userId);
        return conversations.stream()
                .map(Conversation::toConversationVo)
                .collect(Collectors.toList());
    }

    @Override
    public void modifyConversation(ConversationParam conversationParam) {
        int affected = conversationManager.updateConversation(Conversation.ofParam(conversationParam));
        if (affected <= 0) {
            throw new BizException(BizErrorCode.MESSAGE_CONVERSATION_NOT_FOUND, "会话不存在或已删除");
        }
    }


    @Override
    @Transactional(transactionManager = "transactionManagerOne", rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId) {
        messageManager.deleteByConversation(conversationId);
        conversationManager.deleteConversation(conversationId);
    }

    /**
     *  查询会话消息历史（树形分支）
     */
    @Override
    public ConversationHistoryVo history(Long conversationId) {
        // 1. 拉取会话与消息列表
        Conversation conversation = conversationManager.selectById(conversationId);
        List<Message> messages = messageManager.listByConversation(conversationId);
        if (CollectionUtils.isEmpty(messages)) {
            Long pointer = Objects.nonNull(conversation) ? conversation.getCurrentMessageId() : null;
            return ConversationHistoryVo.builder()
                    .currentMessageId(pointer)
                    .messages(Collections.emptyList())
                    .build();
        }

        // 2. 转换为 VO 并构建树形结构
        List<MessageVo> messageVos = messages.stream()
                .map(Message::toMessageVo)
                .collect(Collectors.toList());
        List<MessageVo> tree = TreeBuildUtil.buildRecursiveTree(
                messageVos,
                MessageVo::getId,
                MessageVo::getParentId,
                (node, children) -> node.setChildList(children),
                null
        );
        sortMessageTree(tree);

        // 3. 计算当前指针，缺省则回退为最新消息
        Long currentPointer = Objects.nonNull(conversation) ? conversation.getCurrentMessageId() : null;
        if (Objects.isNull(currentPointer)) {
            Message lastMessage = messages.get(messages.size() - 1);
            currentPointer = lastMessage.getId();
        }

        return ConversationHistoryVo.builder()
                .currentMessageId(currentPointer)
                .messages(tree)
                .build();
    }

    /**
     * 递归排序树形消息，保证子节点按时间与ID有序
     *
     * @param nodes 当前层节点
     */
    private void sortMessageTree(List<MessageVo> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        // 1. 当前层排序
        nodes.sort(Comparator
                .comparing(MessageVo::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MessageVo::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        // 2. 递归对子节点排序
        nodes.forEach(node -> sortMessageTree(node.getChildList()));
    }
    /**
     * 查询单个会话视图
     */
    @Override
    public ConversationVo getConversation(Long conversationId) {
        Conversation conversation = conversationManager.selectById(conversationId);
        return Conversation.toConversationVo(conversation);
    }


    /**
     * 退出登录：删除 Redis 中的会话索引与数据
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
     * 根据主键获取用户信息。
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

    private SessionVo.UserView buildUserView(User user) {
        if (Objects.isNull(user)) {
            return null;
        }
        return SessionVo.UserView.builder()
                .username(user.getUserCode())
                .displayName(StringUtils.hasText(user.getUserName()) ? user.getUserName() : user.getUserCode())
                .build();
    }



    private Message persistUserMessage(Long userId, MessageSendParam param) {
        LocalDateTime now = LocalDateTime.now();
        Message message = Message.builder()
                .conversationId(param.getConversationId())
                .senderId(userId)
                .content(param.getContent())
                .contentType(StringUtils.hasText(param.getContentType()) ? param.getContentType() : "TEXT")
                .status(1)
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
                .build();
        messageManager.insertMessage(assistantMessage);
        return assistantMessage;
    }
}
