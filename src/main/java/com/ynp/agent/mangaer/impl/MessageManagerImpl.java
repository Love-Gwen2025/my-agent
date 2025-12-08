package com.ynp.agent.mangaer.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ynp.agent.mangaer.BaseManager;
import com.ynp.agent.mangaer.MessageManager;
import com.ynp.agent.model.domain.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

@Service
public class MessageManagerImpl extends BaseManager implements MessageManager {

    @Override
    public Message insertMessage(Message message) {
        messageMapper.insert(message);
        return message;
    }

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
}
