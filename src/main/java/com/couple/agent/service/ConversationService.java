package com.couple.agent.service;


import com.couple.agent.model.param.ConversationParam;
import com.couple.agent.model.vo.ChatReplyVo;
import com.couple.agent.model.vo.ConversationVo;
import com.couple.agent.model.vo.HistoryMessageVo;
import com.couple.agent.model.vo.MessageVo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface ConversationService  {


    List<ConversationVo> listConversations(Long id);

    Long createConversation( String title);

    void modifyConversation(ConversationParam conversationParam);

    void deleteConversation( Long conversationId);

    List<MessageVo> history(Long conversationId);

    ConversationVo getConversation( Long conversationId);
}