package com.ynp.agent.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.ynp.agent.domain.entity.ConversationEntity;
import com.ynp.agent.mapper.ConversationMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 会话数据访问。
 */
@Service
public class ConversationManager extends BaseManager {

    private final ConversationMapper conversationMapper;

    public ConversationManager(ConversationMapper conversationMapper) {
        this.conversationMapper = conversationMapper;
    }

    /**
     * 1. 按用户列出会话，更新时间倒序。
     */
    public List<ConversationEntity> listByUser(Long userId) {
        LambdaQueryWrapper<ConversationEntity> wrapper = new LambdaQueryWrapper<>();
        /* 1. 按用户过滤并按更新时间倒序 */
        wrapper.eq(ConversationEntity::getUserId, userId)
                .orderByDesc(ConversationEntity::getUpdatedAt);
        return conversationMapper.selectList(wrapper);
    }

    /**
     * 1. 创建会话。
     */
    public ConversationEntity createConversation(Long userId, String title) {
        ConversationEntity entity = new ConversationEntity();
        entity.setUserId(userId);
        entity.setTitle(title);
        /* 1. 插入会话 */
        conversationMapper.insert(entity);
        return entity;
    }

    /**
     * 1. 更新会话标题。
     */
    public ConversationEntity updateTitle(Long conversationId, String title) {
        ConversationEntity entity = conversationMapper.selectById(conversationId);
        if (Objects.isNull(entity)) {
            return null;
        }
        /* 1. 更新标题后保存 */
        entity.setTitle(title);
        conversationMapper.updateById(entity);
        return entity;
    }

    /**
     * 1. 删除会话。
     */
    public void deleteById(Long conversationId) {
        conversationMapper.deleteById(conversationId);
    }

    /**
     * 1. 根据会话和用户校验访问权限。
     */
    public ConversationEntity findByIdAndUser(Long conversationId, Long userId) {
        LambdaQueryWrapper<ConversationEntity> wrapper = new LambdaQueryWrapper<>();
        /* 1. 按会话与用户匹配 */
        wrapper.eq(ConversationEntity::getId, conversationId)
                .eq(ConversationEntity::getUserId, userId);
        return conversationMapper.selectOne(wrapper);
    }

    /**
     * 1. 获取单个会话。
     */
    public ConversationEntity findById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    /**
     * 1. 更新会话的更新时间戳。
     */
    public void touch(Long conversationId) {
        ConversationEntity entity = conversationMapper.selectById(conversationId);
        if (Objects.isNull(entity)) {
            return;
        }
        conversationMapper.updateById(entity);
    }

    /**
     * 1. 分页查询（保留扩展）。
     */
    public IPage<ConversationEntity> pageByUser(Long userId, long pageNo, long pageSize) {
        LambdaQueryWrapper<ConversationEntity> wrapper = new LambdaQueryWrapper<>();
        /* 1. 分页查询会话 */
        wrapper.eq(ConversationEntity::getUserId, userId)
                .orderByDesc(ConversationEntity::getUpdatedAt);
        return conversationMapper.selectPage(Page.of(pageNo, pageSize), wrapper);
    }
}
