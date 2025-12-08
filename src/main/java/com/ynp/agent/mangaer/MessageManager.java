package com.ynp.agent.mangaer;


import com.ynp.agent.model.domain.Message;

public interface MessageManager {

    /**
     * 插入消息记录。
     *
     * @param message 消息实体
     * @return 填充主键后的消息实体
     */
    Message insertMessage(Message message);
}
