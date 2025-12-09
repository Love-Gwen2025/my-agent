package com.couple.agent.service.impl;

import com.couple.agent.exception.BizErrorCode;
import com.couple.agent.exception.BizException;
import com.couple.agent.model.domain.Conversation;
import com.couple.agent.model.domain.CurrentSession;
import com.couple.agent.model.domain.Message;
import com.couple.agent.model.param.MessageSendParam;
import com.couple.agent.model.vo.MessageVo;
import com.couple.agent.service.BaseService;
import com.couple.agent.service.MessageService;
import com.couple.agent.utils.SessionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 消息服务实现：负责校验成员资格、持久化消息并推送给订阅者。
 */
@Service
public class MessageServiceImpl extends BaseService implements MessageService {
    @Override
    @Transactional(transactionManager = "transactionManagerOne", rollbackFor = Exception.class)
    public MessageVo send(MessageSendParam param) {
        CurrentSession session = SessionUtil.get();
        if (Objects.isNull(session)) {
            throw new BizException(BizErrorCode.AUTH_UNAUTHORIZED, "用户未登录或会话已过期");
        }
        Long senderId = session.getId();
        // 1. 校验会话是否存在
        Conversation conversation = conversationManager.selectById(param.getConversationId());
        if (Objects.isNull(conversation)) {
            throw new BizException(BizErrorCode.MESSAGE_CONVERSATION_NOT_FOUND, "会话不存在或已被删除");
        }
        // 2. 校验当前用户是否为会话拥有者
        if (!Objects.equals(conversation.getUserId(), senderId)) {
            throw new BizException(BizErrorCode.MESSAGE_FORBIDDEN, "您不在该会话中，无法发送消息");
        }

        // 3. 构造消息实体并写入数据库
        String contentType = StringUtils.hasText(param.getContentType()) ? param.getContentType() : "TEXT";
        LocalDateTime now = LocalDateTime.now();
        Message message = Message.builder()
                .conversationId(param.getConversationId())
                .senderId(senderId)
                .content(param.getContent())
                .contentType(contentType)
                .replyTo(param.getReplyTo())
                .status(1)
                .sendTime(now)
                .build();
        messageManager.insertMessage(message);

        // 4. 更新会话最近消息信息
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageAt(now);
        int result= conversationManager.updateById(conversation);
        if (result<=0){
            throw  new BizException(BizErrorCode.MESSAGE_CONVERSATION_NOT_FOUND);
        }
        // 5. 组装返回 / 推送对象
        MessageVo view = MessageVo.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .contentType(message.getContentType())
                .replyTo(message.getReplyTo())
                .sendTime(message.getSendTime())
                .build();

        // 6. 推送给订阅者：单聊/机器人使用点对点发送，群聊维持广播
        messagingTemplate.convertAndSendToUser(
                String.valueOf(conversation.getUserId()),
                "/queue/conversation." + message.getConversationId(),
                view
        );

        return view;
    }
}
