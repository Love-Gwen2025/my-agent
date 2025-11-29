package com.ynp.agent.service;

import com.ynp.agent.config.AppProperties;
import com.ynp.agent.domain.entity.ConversationEntity;
import com.ynp.agent.domain.entity.UserEntity;
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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * 启动初始化服务。
 */
@Service
public class InitService extends BaseService {

    public InitService(AppProperties appProperties,
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
        super(appProperties, userManager, conversationManager, messageManager, attachmentManager, passwordEncoder, jwtHelper, stringRedisTemplate, mongoTemplate, aiService, fileStorageService);
    }

    /**
     * 1. 确保配置账号写入数据库并具备默认会话。
     */
    public void ensureAccounts() {
        for (AppProperties.UserAccount account : appProperties.getSafeAccounts()) {
            String username = account.getUsername();
            UserEntity entity = userManager.findByUsername(username);
            if (Objects.isNull(entity)) {
                /* 1. 不存在时创建用户与默认会话 */
                UserEntity created = new UserEntity();
                created.setUsername(username);
                created.setDisplayName(account.getDisplayName());
                created.setPasswordHash(passwordEncoder.encode(account.getPassword()));
                userManager.createUser(created);
                createDefaultConversation(created.getId(), account.getDisplayName());
                continue;
            }
            /* 2. 同步展示名与密码，保证配置生效 */
            syncDisplayName(entity, account.getDisplayName());
            syncPassword(entity, account.getPassword());
            /* 3. 确保默认会话存在 */
            createDefaultConversation(entity.getId(), account.getDisplayName());
        }
    }

    /**
     * 1. 同步展示名。
     */
    private void syncDisplayName(UserEntity entity, String displayName) {
        if (Objects.equals(displayName, entity.getDisplayName())) {
            return;
        }
        userManager.updateDisplayName(entity.getId(), displayName);
    }

    /**
     * 1. 同步密码哈希。
     */
    private void syncPassword(UserEntity entity, String rawPassword) {
        if (passwordEncoder.matches(rawPassword, entity.getPasswordHash())) {
            return;
        }
        /* 1. 将新密码写回数据库 */
        String newHash = passwordEncoder.encode(rawPassword);
        userManager.updatePassword(entity.getId(), newHash);
    }

    /**
     * 1. 创建默认会话。
     */
    private void createDefaultConversation(Long userId, String displayName) {
        List<ConversationEntity> conversations = conversationManager.listByUser(userId);
        if (!CollectionUtils.isEmpty(conversations)) {
            return;
        }
        String title = displayName + "的第一个会话";
        conversationManager.createConversation(userId, title);
    }
}
