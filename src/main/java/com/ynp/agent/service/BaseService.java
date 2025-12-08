package com.ynp.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ynp.agent.config.JwtProperties;
import com.ynp.agent.config.OssProperties;
import com.ynp.agent.converter.UserConverter;
import com.ynp.agent.mangaer.ConversationManager;
import com.ynp.agent.mangaer.MessageManager;
import com.ynp.agent.mangaer.UserManager;
import com.ynp.agent.utils.JwtUtil;
import com.ynp.agent.utils.OSSSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BaseService {

    // manager 层统一注入，业务层只通过 manager 访问数据库
    @Autowired
    protected ConversationManager conversationManager;
    @Autowired
    protected UserManager userManager;
    @Autowired
    protected MessageManager messageManager;

    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JwtProperties jwtProperties;

    // 工具类依赖集中管理
    @Autowired
    protected JwtUtil jwtUtil;
    @Autowired
    protected OSSSUtil osssUtil;
    @Autowired
    protected StringRedisTemplate redisTemplate;
    @Autowired
    protected RedisScript<Long> loginLimitScript;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected OssProperties ossProperties;
    @Autowired
    protected SimpMessagingTemplate messagingTemplate;

    //转换器
    @Autowired
    protected UserConverter userConverter;
}
