package com.ynp.agent.service.impl;


import com.ynp.agent.exception.BizErrorCode;
import com.ynp.agent.exception.BizException;
import com.ynp.agent.model.domain.Conversation;
import com.ynp.agent.model.param.ConversationParam;
import com.ynp.agent.service.BaseService;
import com.ynp.agent.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl extends BaseService implements ConversationService {

    @Override
    @Transactional(transactionManager = "transactionManagerOne", rollbackFor = Exception.class)
    public Long create(ConversationParam inParam) {
        // 1. 基础参数校验：会话类型与成员列表均不能为空
        Integer type = inParam.getType();
        if (Objects.isNull(type)) {
            throw new BizException(BizErrorCode.CONVERSATION_TYPE_EMPTY, "会话类型不能为空");
        }
        List<Long> rawMemberIds = inParam.getMemberIds();
        if (Objects.isNull(rawMemberIds) || rawMemberIds.isEmpty()) {
            throw new BizException(BizErrorCode.CONVERSATION_MEMBER_EMPTY, "会话成员不能为空");
        }
        // 2. 过滤空值并去重，避免脏数据导致的重复建群
        List<Long> memberIds = rawMemberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (memberIds.isEmpty()) {
            throw new BizException(BizErrorCode.CONVERSATION_MEMBER_EMPTY, "会话成员不能为空");
        }
        if (!userManager.allExist(memberIds)) {
            throw new BizException(BizErrorCode.CONVERSATION_MEMBER_NOT_FOUND, "存在不存在的用户");
        }

        // 3. 单聊 / 机器人会话：需要保证唯一性
        if (Objects.equals(type, 1) || Objects.equals(type, 3)) {
            if (Objects.equals(type, 1) && memberIds.size() != 2) {
                throw new BizException(BizErrorCode.CONVERSATION_MEMBER_EMPTY, "单聊会话必须且只能包含两名有效成员");
            }
            Conversation conversation = conversationManager.selectByMembersAndType(type, memberIds);
            if (Objects.nonNull(conversation)) {
                return conversation.getId();
            }
            Conversation newConversation = Conversation.builder()
                    .type(type)
                    .build();
            Long newConversationId = conversationManager.insertConversation(newConversation);
            conversationManager.insertMembers(newConversationId, memberIds);
            return newConversationId;
        }

        // 4. 群聊等会话：允许重复创建，直接落库
        Conversation newConversation = Conversation.builder()
                .type(type)
                .build();
        Long newConversationId = conversationManager.insertConversation(newConversation);
        conversationManager.insertMembers(newConversationId, memberIds);
        return newConversationId;
    }
}
