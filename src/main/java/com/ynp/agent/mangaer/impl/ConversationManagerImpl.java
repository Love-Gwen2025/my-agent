package com.ynp.agent.mangaer.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.ynp.agent.mangaer.BaseManager;
import com.ynp.agent.mangaer.ConversationManager;
import com.ynp.agent.model.domain.Conversation;
import com.ynp.agent.model.domain.ConversationMember;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConversationManagerImpl extends BaseManager implements ConversationManager {

    @Override
    public Long insertConversation(Conversation conversation) {
        conversationMapper.insert(conversation);
        return conversation.getId();
    }

    @Override
    public void insertMembers(Long conversationId, List<Long> memberIds) {
        if (CollectionUtils.isEmpty(memberIds)) {
            return;
        }
        memberIds.forEach(memberId -> {
            ConversationMember member = ConversationMember.builder()
                    .conversationId(conversationId)
                    .userId(memberId)
                    .status(1)
                    .build();
            conversationMemberMapper.insert(member);
        });
    }

    @Override
    public Conversation selectById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    @Override
    public boolean existsMember(Long conversationId, Long userId) {
        return conversationMemberMapper.selectCount(
                new MPJLambdaWrapper<ConversationMember>()
                        .eq(ConversationMember::getConversationId, conversationId)
                        .eq(ConversationMember::getUserId, userId)
                        .eq(ConversationMember::getStatus, 1)
        ) > 0;
    }

    @Override
    public void updateLastMessage(Long conversationId, Long messageId, LocalDateTime sendTime) {
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .set(Conversation::getLastMessageId, messageId)
                .set(Conversation::getLastMessageAt, sendTime));
    }

    @Override
    public Conversation selectByMembersAndType(Integer type, List<Long> memberIds) {
        // 入参兜底：会话类型或成员列表为空时直接返回空，避免无谓的数据库查询
        if (Objects.isNull(type) || CollectionUtils.isEmpty(memberIds)) {
            return null;
        }
        // 过滤空值并去重，保证比较逻辑稳定
        List<Long> distinctMemberIds = memberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(distinctMemberIds)) {
            return null;
        }
        int size = distinctMemberIds.size();
        MPJLambdaWrapper<Conversation> wrapper = new MPJLambdaWrapper<>();
        wrapper.innerJoin(ConversationMember.class,
                        ConversationMember::getConversationId, Conversation::getId)
                .eq(Conversation::getType, type)
                .eq(ConversationMember::getStatus, 1)
                .in(ConversationMember::getUserId, distinctMemberIds)
                .groupBy(Conversation::getId)
                // 只保留同时包含所有目标成员的会话
                .having("COUNT(DISTINCT t1.user_id) = {0}", size)
                .orderByDesc(Conversation::getCreateTime);

        List<Conversation> candidateList = conversationMapper.selectJoinList(Conversation.class, wrapper);
        for (Conversation conversation : candidateList) {
            List<Long> activeMemberIds = listActiveMemberIds(conversation.getId());
            Set<Long> activeSet = new HashSet<>(activeMemberIds);
            Set<Long> targetSet = new HashSet<>(distinctMemberIds);
            if (Objects.equals(activeSet.size(), targetSet.size()) && activeSet.containsAll(targetSet)) {
                return conversation;
            }
        }
        return null;
    }

    @Override
    public List<Long> listActiveMemberIds(Long conversationId) {
        // 会话ID为空时直接返回空列表，避免继续访问数据库
        if (Objects.isNull(conversationId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ConversationMember> query = new LambdaQueryWrapper<ConversationMember>()
                .select(ConversationMember::getUserId)
                .eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getStatus, 1);
        return conversationMemberMapper.selectList(query)
                .stream()
                .map(ConversationMember::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
        MPJLambdaWrapper<Conversation> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(Conversation.class)
                .innerJoin(ConversationMember.class, ConversationMember::getConversationId, Conversation::getId)
                .eq(ConversationMember::getUserId, userId)
                .eq(ConversationMember::getStatus, 1)
                .orderByDesc(Conversation::getLastMessageAt, Conversation::getCreateTime);
        return conversationMapper.selectJoinList(Conversation.class, wrapper);
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
    public void deleteMembers(Long conversationId) {
        if (Objects.isNull(conversationId)) {
            return;
        }
        LambdaQueryWrapper<ConversationMember> queryWrapper = new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId);
        conversationMemberMapper.delete(queryWrapper);
    }
}
