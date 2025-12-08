package com.ynp.agent.store;


import com.ynp.agent.model.domain.ChatMemoryMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MongoMemoryMessageStore implements ChatMemoryStore {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    //这里的message是AI message，UserMessage的父类
    public List<ChatMessage> getMessages(Object memoryId) {
        //设置查询条件
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        //从db中查一个
        ChatMemoryMessage one = mongoTemplate.findOne(query, ChatMemoryMessage.class);
        if (one == null) {
            return new ArrayList<>();
        }
        //得到聊天内容
        String content = one.getContent();
        //反序列化为ChatMessage
        return ChatMessageDeserializer.messagesFromJson(content);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        Update update = new Update();
        String messagesToJson = ChatMessageSerializer.messagesToJson(list);
        update.set("content", messagesToJson);
        mongoTemplate.upsert(query, update, ChatMemoryMessage.class);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        mongoTemplate.remove(new Query(criteria), ChatMessage.class);
    }
}
