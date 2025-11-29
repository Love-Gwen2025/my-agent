package com.ynp.agent.service;

import com.ynp.agent.config.AppProperties;
import com.ynp.agent.converter.UserConverter;
import com.ynp.agent.domain.entity.ConversationEntity;
import com.ynp.agent.domain.entity.UserEntity;
import com.ynp.agent.exception.ServiceException;
import com.ynp.agent.helper.JwtHelper;
import com.ynp.agent.helper.UserContextHolder;
import com.ynp.agent.helper.model.JwtPayload;
import com.ynp.agent.manager.AttachmentManager;
import com.ynp.agent.manager.ConversationManager;
import com.ynp.agent.manager.MessageManager;
import com.ynp.agent.manager.UserManager;
import com.ynp.agent.service.ai.AiService;
import com.ynp.agent.service.file.FileStorageService;
import com.ynp.agent.vo.AccountVO;
import com.ynp.agent.vo.SessionVO;
import com.ynp.agent.vo.UserVO;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 认证与账号服务。
 */
@Service
public class AuthService extends BaseService {

    private static final String TOKEN_KEY_PREFIX = "agent:token:";

    private final UserConverter userConverter;

    public AuthService(AppProperties appProperties,
                       UserManager userManager,
                       ConversationManager conversationManager,
                       MessageManager messageManager,
                       AttachmentManager attachmentManager,
                       PasswordEncoder passwordEncoder,
                       JwtHelper jwtHelper,
                       StringRedisTemplate stringRedisTemplate,
                       MongoTemplate mongoTemplate,
                       AiService aiService,
                       FileStorageService fileStorageService,
                       UserConverter userConverter) {
        super(appProperties, userManager, conversationManager, messageManager, attachmentManager, passwordEncoder, jwtHelper, stringRedisTemplate, mongoTemplate, aiService, fileStorageService);
        this.userConverter = userConverter;
    }

    /**
     * 1. 获取登录态信息，未登录返回空态。
     */
    public SessionVO fetchSession(JwtPayload payload) {
        SessionVO vo = new SessionVO();
        /* 1. 写入助手名称与账号列表 */
        vo.setAssistantName(appProperties.getAssistantDisplayName());
        vo.setAccounts(buildAccounts());
        if (Objects.isNull(payload)) {
            /* 2. 未登录直接返回空态 */
            vo.setAuthenticated(false);
            return vo;
        }
        /* 3. 已登录填充用户信息 */
        UserVO userVO = new UserVO();
        userVO.setUsername(payload.getUsername());
        userVO.setDisplayName(payload.getDisplayName());
        vo.setUser(userVO);
        vo.setAuthenticated(true);
        return vo;
    }

    /**
     * 1. 登录并返回 token。
     */
    public String login(String username, String rawPassword) {
        /* 1. 归一化用户名方便匹配 */
        String normalized = username.trim().toLowerCase();
        Optional<AppProperties.UserAccount> configured = appProperties.getSafeAccounts()
                .stream()
                .filter(acc -> normalized.equals(acc.getUsername()))
                .findFirst();
        if (configured.isEmpty()) {
            throw new ServiceException(401, "账号或密码错误");
        }
        AppProperties.UserAccount account = configured.get();
        /* 2. 拉取或创建用户实体 */
        UserEntity entity = userManager.findByUsername(normalized);
        if (Objects.isNull(entity)) {
            entity = createUser(account);
        }
        /* 3. 校验密码 */
        if (!passwordEncoder.matches(rawPassword, entity.getPasswordHash())) {
            throw new ServiceException(401, "账号或密码错误");
        }
        /* 4. 确保默认会话存在后生成 token */
        ensureDefaultConversation(entity.getId(), account.getDisplayName());
        String token = jwtHelper.generateToken(entity.getId(), normalized, account.getDisplayName());
        /* 5. 将 token 缓存至 Redis，便于登出与失效控制 */
        cacheToken(token, entity.getId());
        return token;
    }

    /**
     * 1. 登出并清理 token。
     */
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        /* 1. 删除 Redis 中的 token 以失效会话 */
        stringRedisTemplate.delete(buildTokenKey(token));
    }

    /**
     * 1. 校验 token 是否有效。
     */
    public JwtPayload validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        /* 1. 解析 JWT 载荷 */
        JwtPayload payload = jwtHelper.parseToken(token);
        if (Objects.isNull(payload)) {
            return null;
        }
        String key = buildTokenKey(token);
        /* 2. 校验 Redis 中是否存在 token，防止已登出仍访问 */
        String val = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(val)) {
            return null;
        }
        return payload;
    }

    /**
     * 1. 构建账户列表。
     */
    private List<AccountVO> buildAccounts() {
        /* 1. 直接映射配置项为展示用账号列表 */
        return appProperties.getSafeAccounts()
                .stream()
                .map(userConverter::fromAccount)
                .collect(Collectors.toList());
    }

    /**
     * 1. 按配置创建用户。
     */
    private UserEntity createUser(AppProperties.UserAccount account) {
        /* 1. 写入用户名、展示名与加密密码 */
        UserEntity entity = new UserEntity();
        entity.setUsername(account.getUsername());
        entity.setDisplayName(account.getDisplayName());
        entity.setPasswordHash(passwordEncoder.encode(account.getPassword()));
        return userManager.createUser(entity);
    }

    /**
     * 1. 确保默认会话存在。
     */
    private void ensureDefaultConversation(Long userId, String displayName) {
        /* 1. 若已存在会话则直接返回 */
        List<ConversationEntity> conversations = conversationManager.listByUser(userId);
        if (!conversations.isEmpty()) {
            return;
        }
        /* 2. 创建默认标题会话 */
        String title = displayName + "的第一个会话";
        conversationManager.createConversation(userId, title);
    }

    /**
     * 1. 将 token 写入 Redis。
     */
    private void cacheToken(String token, Long userId) {
        /* 1. 以 token 为键写入用户 ID 并设置 TTL */
        String key = buildTokenKey(token);
        Duration ttl = Duration.ofMinutes(appProperties.getJwtExpireMinutes());
        stringRedisTemplate.opsForValue().set(key, String.valueOf(userId), ttl);
    }

    /**
     * 1. 构建 Redis key。
     */
    private String buildTokenKey(String token) {
        /* 1. 拼接统一的 token Key 前缀 */
        return TOKEN_KEY_PREFIX + token;
    }
}
