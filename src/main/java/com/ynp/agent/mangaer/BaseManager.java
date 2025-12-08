package com.ynp.agent.mangaer;


import com.ynp.agent.mapper.ConversationMapper;
import com.ynp.agent.mapper.ConversationMemberMapper;
import com.ynp.agent.mapper.MessageMapper;
import com.ynp.agent.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseManager {
    @Autowired
    protected UserMapper userMapper;
    @Autowired
    protected ConversationMapper conversationMapper;
    @Autowired
    protected ConversationMemberMapper conversationMemberMapper;
    @Autowired
    protected MessageMapper messageMapper;
}
