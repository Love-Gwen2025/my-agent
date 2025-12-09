package com.couple.agent.controller;


import com.couple.agent.assistant.LoveAssistant;
import com.couple.agent.converter.UserConverter;
import com.couple.agent.service.AiChatService;
import com.couple.agent.service.ChatApiService;
import com.couple.agent.service.ConversationService;
import com.couple.agent.service.MessageService;
import com.couple.agent.service.ModelSelectorService;
import com.couple.agent.service.UserService;
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
