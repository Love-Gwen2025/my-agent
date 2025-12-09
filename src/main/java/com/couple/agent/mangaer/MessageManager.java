package com.couple.agent.mangaer;


import com.couple.agent.model.domain.Message;

import java.util.List;

/**
 * 消息管理接口
 *
 * <p>负责消息相关的数据库操作</p>
 *
 * @author ynp
 */
public interface MessageManager {

    /**
     * 插入消息记录
     *
     * @param message 消息实体
     * @return 填充主键后的消息实体
     */
    Message insertMessage(Message message);

    /**
     * 查询会话的消息列表，按发送时间顺序返回
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<Message> listByConversation(Long conversationId);

    /**
     * 删除会话下的所有消息
     *
     * @param conversationId 会话ID
     * @return 受影响行数
     */
    int deleteByConversation(Long conversationId);

    /**
     * 查询会话的最后一条消息
     *
     * @param conversationId 会话ID
     * @return 最后一条消息，如果没有则返回 null
     */
    Message selectLastMessage(Long conversationId);
}
