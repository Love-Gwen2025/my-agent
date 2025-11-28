package com.ynp.agent.service;

import com.ynp.agent.config.AppProperties;
import com.ynp.agent.helper.JwtHelper;
import com.ynp.agent.manager.AttachmentManager;
import com.ynp.agent.manager.ConversationManager;
import com.ynp.agent.manager.MessageManager;
import com.ynp.agent.manager.UserManager;
import com.ynp.agent.service.ai.AiService;
import com.ynp.agent.service.file.FileStorageService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Service 层基类，集中注入所有依赖。
 */
public abstract class BaseService {

    protected final AppProperties appProperties;
    protected final UserManager userManager;
    protected final ConversationManager conversationManager;
    protected final MessageManager messageManager;
    protected final AttachmentManager attachmentManager;
    protected final PasswordEncoder passwordEncoder;
    protected final JwtHelper jwtHelper;
    protected final StringRedisTemplate stringRedisTemplate;
    protected final MongoTemplate mongoTemplate;
    protected final AiService aiService;
    protected final FileStorageService fileStorageService;

    protected BaseService(AppProperties appProperties,
                          UserManager userManager,
                          ConversationManager conversationManager,
                          MessageManager messageManager,
                          AttachmentManager attachmentManager,
                          PasswordEncoder passwordEncoder,
                          JwtHelper jwtHelper,
                          StringRedisTemplate stringRedisTemplate,
                          MongoTemplate mongoTemplate,
                          AiService aiService,
                          FileStorageService fileStorageService) {
        this.appProperties = appProperties;
        this.userManager = userManager;
        this.conversationManager = conversationManager;
        this.messageManager = messageManager;
        this.attachmentManager = attachmentManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtHelper = jwtHelper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.mongoTemplate = mongoTemplate;
        this.aiService = aiService;
        this.fileStorageService = fileStorageService;
    }
}
