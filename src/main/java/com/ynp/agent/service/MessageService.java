package com.ynp.agent.service;


import com.ynp.agent.model.param.MessageSendParam;
import com.ynp.agent.model.vo.MessageVo;

public interface MessageService {

    /**
     * 发送消息。
     *
     * @param param 消息参数
     * @return 消息视图
     */
    MessageVo send(MessageSendParam param);
}
