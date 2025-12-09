package com.couple.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.couple.agent.assistant.LoveAssistant;
import com.couple.agent.config.JwtProperties;
import com.couple.agent.config.OssProperties;
import com.couple.agent.converter.UserConverter;
import com.couple.agent.mangaer.ConversationManager;
import com.couple.agent.mangaer.MessageManager;
import com.couple.agent.mangaer.UserManager;
import com.couple.agent.store.RedisChatMemoryStore;
import com.couple.agent.utils.JwtUtil;
import com.couple.agent.utils.OSSSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Service 基类
 *
 * <p>集中管理业务层的依赖注入，所有 Service 实现类继承此类</p>
 *
 * @author ynp
 */
public class BaseService {

    // ==================== Manager 层依赖 ====================

    /**
     * 会话管理器
     */
    @Autowired
    protected ConversationManager conversationManager;

    /**
     * 用户管理器
     */
    @Autowired
    protected UserManager userManager;

    /**
     * 消息管理器
     */
    @Autowired
    protected MessageManager messageManager;

    // ==================== 安全相关依赖 ====================

    /**
     * 密码编码器
     */
    @Autowired
    protected PasswordEncoder passwordEncoder;

    /**
     * JWT 配置属性
     */
    @Autowired
    protected JwtProperties jwtProperties;

    // ==================== 工具类依赖 ====================

    /**
     * JWT 工具类
     */
    @Autowired
    protected JwtUtil jwtUtil;

    /**
     * OSS 工具类
     */
    @Autowired
    protected OSSSUtil osssUtil;

    /**
     * Redis 模板
     */
    @Autowired
    protected StringRedisTemplate redisTemplate;

    /**
     * 登录限制 Lua 脚本
     */
    @Autowired
    protected RedisScript<Long> loginLimitScript;

    /**
     * JSON 序列化工具
     */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * OSS 配置属性
     */
    @Autowired
    protected OssProperties ossProperties;

    /**
     * WebSocket 消息模板
     */
    @Autowired
    protected SimpMessagingTemplate messagingTemplate;

    // ==================== 转换器依赖 ====================

    /**
     * 用户对象转换器
     */
    @Autowired
    protected UserConverter userConverter;

    // ==================== AI 相关依赖 ====================

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

    /**
     * Redis 聊天记忆存储
     */
    @Autowired
    protected RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 向量嵌入服务
     */
    @Autowired
    protected EmbeddingService embeddingService;
}
