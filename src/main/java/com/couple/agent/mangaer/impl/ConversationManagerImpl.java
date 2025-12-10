package com.couple.agent.mangaer.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.couple.agent.mangaer.BaseManager;
import com.couple.agent.mangaer.ConversationManager;
import com.couple.agent.model.domain.Conversation;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class ConversationManagerImpl extends BaseManager implements ConversationManager {

    @Override
    public Long insertConversation(Conversation conversation) {
        conversationMapper.insert(conversation);
        return conversation.getId();
    }

    @Override
    public Conversation selectById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    @Override
    public void updateLastMessage(Long conversationId, Long messageId, LocalDateTime sendTime) {
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .set(Conversation::getLastMessageId, messageId)
                .set(Conversation::getLastMessageAt, sendTime));
    }

    @Override
    public int updateById(Conversation conversation) {
        return conversationMapper.updateById(conversation);
    }

    @Override
    public List<Conversation> listByUser(Long userId) {
        if (Objects.isNull(userId)) {
            return Collections.emptyList();
        }
        return conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getLastMessageAt, Conversation::getCreateTime)
        );
    }

    @Override
    public int updateTitle(Long conversationId, String title) {
        if (Objects.isNull(conversationId)) {
            return 0;
        }
        LambdaUpdateWrapper<Conversation> updateWrapper = new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .set(StringUtils.hasText(title), Conversation::getTitle, title);
        return conversationMapper.update(null, updateWrapper);
    }

    @Override
    public int deleteConversation(Long conversationId) {
        if (Objects.isNull(conversationId)) {
            return 0;
        }
        return conversationMapper.deleteById(conversationId);
    }

    @Override
    public int updateConversation(Conversation conversation) {
        return conversationMapper.updateById(conversation);
    }
}
