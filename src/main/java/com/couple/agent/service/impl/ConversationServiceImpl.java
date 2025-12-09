package com.couple.agent.service.impl;


import com.couple.agent.exception.BizErrorCode;
import com.couple.agent.exception.BizException;
import com.couple.agent.model.domain.Conversation;
import com.couple.agent.model.param.ConversationParam;
import com.couple.agent.service.BaseService;
import com.couple.agent.service.ConversationService;
import com.couple.agent.utils.SessionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ConversationServiceImpl extends BaseService implements ConversationService {

    @Override
    @Transactional(transactionManager = "transactionManagerOne", rollbackFor = Exception.class)
    public Long create(ConversationParam inParam) {
        // 1. 获取当前登录用户，AI 对话仅绑定会话拥有者
        var session = SessionUtil.get();
        if (Objects.isNull(session) || Objects.isNull(session.getId())) {
            throw new BizException(BizErrorCode.AUTH_UNAUTHORIZED, "会话创建前需要登录");
        }
        // 2. 构建会话，绑定当前用户
        Conversation newConversation = Conversation.builder()
                .userId(session.getId())
                .title(inParam.getTitle())
                .build();
        // 3. 入库
        return conversationManager.insertConversation(newConversation);
    }
}
