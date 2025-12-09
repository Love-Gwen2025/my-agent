package com.ynp.agent.mangaer.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ynp.agent.mangaer.BaseManager;
import com.ynp.agent.mangaer.MessageManager;
import com.ynp.agent.model.domain.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 消息管理实现类
 *
 * @author ynp
 */
@Service
public class MessageManagerImpl extends BaseManager implements MessageManager {

    /**
     * 插入消息记录
     */
    @Override
    public Message insertMessage(Message message) {
        messageMapper.insert(message);
        return message;
    }

    /**
     * 查询会话的消息列表，按发送时间顺序返回
     */
    @Override
    public List<Message> listByConversation(Long conversationId) {
        if (ObjectUtils.isEmpty(conversationId)) {
            return Collections.emptyList();
        }
        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getSendTime, Message::getId)
        );
    }

    /**
     * 删除会话下的所有消息
     */
    @Override
    public int deleteByConversation(Long conversationId) {
        if (ObjectUtils.isEmpty(conversationId)) {
            return 0;
        }
        return messageMapper.delete(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
        );
    }

    /**
     * 查询会话的最后一条消息
     */
    @Override
    public Message selectLastMessage(Long conversationId) {
        if (Objects.isNull(conversationId)) {
            return null;
        }
        return messageMapper.selectOne(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByDesc(Message::getSendTime, Message::getId)
                        .last("LIMIT 1")
        );
    }
}
