package com.ynp.agent.controller;


import com.ynp.agent.assistant.LoveAssistant;
import com.ynp.agent.converter.UserConverter;
import com.ynp.agent.service.ChatApiService;
import com.ynp.agent.service.ConversationService;
import com.ynp.agent.service.MessageService;
import com.ynp.agent.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 控制器通用父类，集中注入业务服务，便于子类直接使用。
 */
public class BaseController {
    @Autowired
    protected UserService userService;
    @Autowired
    protected ConversationService conversationService;
    @Autowired
    protected MessageService messageService;
    @Autowired
    protected ChatApiService chatApiService;


    /**
     * 用户实体转换器，提供参数与视图对象的互转功能。
     */
    @Autowired
    protected UserConverter userConverter;

    @Autowired
    protected LoveAssistant assistant;
}
