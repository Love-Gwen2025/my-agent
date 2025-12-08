package com.ynp.agent.mangaer;


import com.ynp.agent.model.domain.Message;

import java.util.List;

public interface MessageManager {

    /**
     * 插入消息记录。
     *
     * @param message 消息实体
     * @return 填充主键后的消息实体
     */
    Message insertMessage(Message message);

    /**
     * 查询会话的消息列表，按发送时间顺序返回。
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<Message> listByConversation(Long conversationId);

    /**
     * 删除会话下的所有消息。
     *
     * @param conversationId 会话ID
     * @return 受影响行数
     */
    int deleteByConversation(Long conversationId);
}
