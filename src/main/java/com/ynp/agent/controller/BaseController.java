package com.ynp.agent.controller;


import com.ynp.agent.assistant.LoveAssistant;
import com.ynp.agent.converter.UserConverter;
import com.ynp.agent.service.AiChatService;
import com.ynp.agent.service.ChatApiService;
import com.ynp.agent.service.ConversationService;
import com.ynp.agent.service.MessageService;
import com.ynp.agent.service.ModelSelectorService;
import com.ynp.agent.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 控制器基类
 *
 * <p>集中注入业务服务，便于子类直接使用</p>
 *
 * @author ynp
 */
public class BaseController {

    // ==================== 业务服务 ====================

    /**
     * 用户服务
     */
    @Autowired
    protected UserService userService;

    /**
     * 会话服务
     */
    @Autowired
    protected ConversationService conversationService;

    /**
     * 消息服务
     */
    @Autowired
    protected MessageService messageService;

    /**
     * 聊天 API 服务（旧版，保留兼容）
     */
    @Autowired
    protected ChatApiService chatApiService;

    /**
     * AI 聊天服务（新版，支持流式）
     */
    @Autowired
    protected AiChatService aiChatService;

    // ==================== 转换器 ====================

    /**
     * 用户实体转换器
     */
    @Autowired
    protected UserConverter userConverter;

    // ==================== AI 相关服务 ====================

    /**
     * 情感助手（保留兼容）
     */
    @Autowired
    protected LoveAssistant assistant;

    /**
     * 模型选择服务
     */
    @Autowired
    protected ModelSelectorService modelSelectorService;
}
