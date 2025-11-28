package com.ynp.agent.service;

import com.ynp.agent.config.AppProperties;
import com.ynp.agent.converter.ConversationConverter;
import com.ynp.agent.domain.entity.ConversationEntity;
import com.ynp.agent.domain.entity.UserEntity;
import com.ynp.agent.exception.ServiceException;
import com.ynp.agent.helper.JwtHelper;
import com.ynp.agent.manager.AttachmentManager;
import com.ynp.agent.manager.ConversationManager;
import com.ynp.agent.manager.MessageManager;
import com.ynp.agent.manager.UserManager;
import com.ynp.agent.service.ai.AiService;
import com.ynp.agent.service.file.FileStorageService;
import com.ynp.agent.vo.ConversationListVO;
import com.ynp.agent.vo.ConversationVO;
import com.ynp.agent.vo.ConversationWrapperVO;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 会话相关业务。
 */
@Service
public class ConversationService extends BaseService {

    private final ConversationConverter conversationConverter;

    public ConversationService(AppProperties appProperties,
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
                               ConversationConverter conversationConverter) {
        super(appProperties, userManager, conversationManager, messageManager, attachmentManager, passwordEncoder, jwtHelper, stringRedisTemplate, mongoTemplate, aiService, fileStorageService);
        this.conversationConverter = conversationConverter;
    }

    /**
     * 1. 获取会话列表，如为空则创建默认会话。
     */
    public ConversationListVO listConversations(Long userId) {
        List<ConversationEntity> list = conversationManager.listByUser(userId);
        if (CollectionUtils.isEmpty(list)) {
            /* 1. 创建默认会话，保证前端有可选项 */
            UserEntity user = userManager.findById(userId);
            String displayName = Objects.isNull(user) ? "用户" : user.getDisplayName();
            ConversationEntity created = conversationManager.createConversation(userId, displayName + "的第一个会话");
            list = List.of(created);
        }
        List<ConversationVO> voList = list.stream().map(conversationConverter::toVO).collect(Collectors.toList());
        return new ConversationListVO(voList);
    }

    /**
     * 1. 创建会话。
     */
    public ConversationWrapperVO createConversation(Long userId, String title) {
        String finalTitle = StringUtils.hasText(title) ? title.trim() : "新的会话";
        /* 1. 写入会话并返回包装数据 */
        ConversationEntity entity = conversationManager.createConversation(userId, finalTitle);
        return new ConversationWrapperVO(conversationConverter.toVO(entity));
    }

    /**
     * 1. 重命名会话。
     */
    public ConversationWrapperVO rename(Long userId, Long conversationId, String title) {
        ConversationEntity entity = conversationManager.findByIdAndUser(conversationId, userId);
        if (Objects.isNull(entity)) {
            throw new ServiceException(404, "对话不存在或没有权限访问");
        }
        /* 1. 更新标题后返回 */
        ConversationEntity updated = conversationManager.updateTitle(conversationId, title.trim());
        return new ConversationWrapperVO(conversationConverter.toVO(updated));
    }

    /**
     * 1. 删除会话、消息与附件文件。
     */
    public void deleteConversation(Long userId, Long conversationId) {
        ConversationEntity entity = conversationManager.findByIdAndUser(conversationId, userId);
        if (Objects.isNull(entity)) {
            throw new ServiceException(404, "对话不存在或没有权限访问");
        }
        /* 1. 先删除物理文件与附件记录 */
        List<String> storedNames = attachmentManager.listStoredNamesByConversation(conversationId);
        fileStorageService.deleteFiles(storedNames);
        attachmentManager.deleteByConversation(conversationId);
        /* 2. 再删除消息与会话 */
        messageManager.deleteByConversation(conversationId);
        conversationManager.deleteById(conversationId);
        /* 3. 清理 MongoDB 中的缓存上下文 */
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is(conversationId));
        mongoTemplate.remove(query, "conversation_cache");
    }

}
