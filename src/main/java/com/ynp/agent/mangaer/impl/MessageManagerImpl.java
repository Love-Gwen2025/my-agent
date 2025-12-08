package com.ynp.agent.mangaer.impl;

import com.ynp.agent.mangaer.BaseManager;
import com.ynp.agent.mangaer.MessageManager;
import com.ynp.agent.model.domain.Message;
import org.springframework.stereotype.Service;

@Service
public class MessageManagerImpl extends BaseManager implements MessageManager {

    @Override
    public Message insertMessage(Message message) {
        messageMapper.insert(message);
        return message;
    }
}
