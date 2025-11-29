package com.ynp.agent.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ynp.agent.domain.entity.MessageEntity;
import com.ynp.agent.mapper.MessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 消息数据访问。
 */
@Service
public class MessageManager extends BaseManager {

    private final MessageMapper messageMapper;

    public MessageManager(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 1. 按会话查询消息。
     */
    public List<MessageEntity> listByConversation(Long userId, Long conversationId) {
        LambdaQueryWrapper<MessageEntity> wrapper = new LambdaQueryWrapper<>();
        /* 1. 按用户与会话过滤，按 ID 正序 */
        wrapper.eq(MessageEntity::getUserId, userId)
                .eq(MessageEntity::getConversationId, conversationId)
                .orderByAsc(MessageEntity::getId);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 1. 查询最近消息。
     */
    public List<MessageEntity> listRecentByConversation(Long userId, Long conversationId, Integer limit) {
        LambdaQueryWrapper<MessageEntity> wrapper = new LambdaQueryWrapper<>();
        int finalLimit = Objects.isNull(limit) || limit <= 0 ? 20 : limit;
        /* 1. 倒序拉取最近消息并限制条数 */
        wrapper.eq(MessageEntity::getUserId, userId)
                .eq(MessageEntity::getConversationId, conversationId)
                .orderByDesc(MessageEntity::getId)
                .last("LIMIT " + finalLimit);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 1. 创建消息。
     */
    public MessageEntity createMessage(Long userId, Long conversationId, String role, String content) {
        MessageEntity entity = new MessageEntity();
        entity.setUserId(userId);
        entity.setConversationId(conversationId);
        entity.setRole(role);
        entity.setContent(content);
        /* 1. 插入消息 */
        messageMapper.insert(entity);
        return entity;
    }

    /**
     * 1. 级联删除会话消息。
     */
    public void deleteByConversation(Long conversationId) {
        LambdaUpdateWrapper<MessageEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MessageEntity::getConversationId, conversationId);
        /* 1. 批量删除会话下的消息 */
        messageMapper.delete(wrapper);
    }
}
