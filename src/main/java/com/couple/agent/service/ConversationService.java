package com.couple.agent.service;


import com.couple.agent.model.param.ConversationParam;
import com.couple.agent.model.vo.ConversationHistoryVo;
import com.couple.agent.model.vo.ConversationVo;

import java.util.List;

public interface ConversationService  {


    List<ConversationVo> listConversations(Long id);

    Long createConversation( String title);

    void modifyConversation(ConversationParam conversationParam);

    void deleteConversation( Long conversationId);

    ConversationHistoryVo history(Long conversationId);

    ConversationVo getConversation( Long conversationId);
}
