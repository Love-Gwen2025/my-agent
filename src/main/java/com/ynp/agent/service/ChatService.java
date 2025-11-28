package com.ynp.agent.service;

import com.ynp.agent.config.AppProperties;
import com.ynp.agent.converter.AttachmentConverter;
import com.ynp.agent.converter.ConversationConverter;
import com.ynp.agent.converter.MessageConverter;
import com.ynp.agent.domain.entity.AttachmentEntity;
import com.ynp.agent.domain.entity.ConversationEntity;
import com.ynp.agent.domain.entity.MessageEntity;
import com.ynp.agent.exception.ServiceException;
import com.ynp.agent.helper.JwtHelper;
import com.ynp.agent.manager.AttachmentManager;
import com.ynp.agent.manager.ConversationManager;
import com.ynp.agent.manager.MessageManager;
import com.ynp.agent.manager.UserManager;
import com.ynp.agent.service.ai.AiService;
import com.ynp.agent.service.file.FileStorageService;
import com.ynp.agent.service.file.StoredFile;
import com.ynp.agent.vo.AttachmentVO;
import com.ynp.agent.vo.ChatReplyVO;
import com.ynp.agent.vo.ConversationVO;
import com.ynp.agent.vo.HistoryMessageVO;
import com.ynp.agent.vo.HistoryResponseVO;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 聊天与历史服务。
 */
@Service
public class ChatService extends BaseService {

    private final MessageConverter messageConverter;
    private final AttachmentConverter attachmentConverter;
    private final ConversationConverter conversationConverter;

    public ChatService(AppProperties appProperties,
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
                       MessageConverter messageConverter,
                       AttachmentConverter attachmentConverter,
                       ConversationConverter conversationConverter) {
        super(appProperties, userManager, conversationManager, messageManager, attachmentManager, passwordEncoder, jwtHelper, stringRedisTemplate, mongoTemplate, aiService, fileStorageService);
        this.messageConverter = messageConverter;
        this.attachmentConverter = attachmentConverter;
        this.conversationConverter = conversationConverter;
    }

    /**
     * 1. 查询历史记录。
     */
    public HistoryResponseVO fetchHistory(Long userId, Long conversationId) {
        ConversationEntity conversation = conversationManager.findByIdAndUser(conversationId, userId);
        if (Objects.isNull(conversation)) {
            throw new ServiceException(404, "对话不存在或没有权限访问");
        }
        /* 1. 查询消息与附件 */
        List<MessageEntity> messages = messageManager.listByConversation(userId, conversationId);
        List<Long> messageIds = messages.stream().map(MessageEntity::getId).toList();
        List<AttachmentEntity> attachments = attachmentManager.listByMessageIds(messageIds);
        Map<Long, List<AttachmentEntity>> attachmentMap = buildAttachmentMap(attachments);
        /* 2. 组装 VO 列表 */
        List<HistoryMessageVO> history = messages.stream().map(msg -> buildHistoryVO(msg, attachmentMap.get(msg.getId()))).collect(Collectors.toList());
        ConversationVO conversationVO = conversationConverter.toVO(conversation);
        return new HistoryResponseVO(conversationVO, history);
    }

    /**
     * 1. 处理聊天请求并返回 AI 回复。
     */
    public ChatReplyVO chat(Long userId, Long conversationId, String message, List<MultipartFile> files) {
        ConversationEntity conversation = conversationManager.findByIdAndUser(conversationId, userId);
        if (Objects.isNull(conversation)) {
            throw new ServiceException(404, "对话不存在或没有权限访问");
        }
        if (!StringUtils.hasText(message) && CollectionUtils.isEmpty(files)) {
            throw new ServiceException(400, "消息或附件至少输入一个");
        }
        if (Objects.nonNull(files) && files.size() > appProperties.getMaxUploadFiles()) {
            throw new ServiceException(400, "上传文件数量超出限制");
        }
        /* 1. 保存用户消息 */
        MessageEntity userMessage = messageManager.createMessage(userId, conversationId, "user", Objects.nonNull(message) ? message.trim() : "");
        /* 2. 保存附件文件与记录 */
        List<AttachmentEntity> savedAttachments = saveAttachments(userMessage.getId(), files);
        List<MessageEntity> recent = messageManager.listRecentByConversation(userId, conversationId, appProperties.getHistoryLimit());
        List<Long> ids = recent.stream().map(MessageEntity::getId).toList();
        Map<Long, List<AttachmentEntity>> recentAttachmentMap = buildAttachmentMap(attachmentManager.listByMessageIds(ids));
        /* 3. 调用 AI 获取回复 */
        String reply = aiService.chat(userId, conversationId, reverseList(recent), recentAttachmentMap);
        /* 4. 保存助手消息并更新时间 */
        messageManager.createMessage(userId, conversationId, "assistant", reply);
        conversationManager.touch(conversationId);
        return new ChatReplyVO(reply);
    }

    /**
     * 1. 保存附件记录与文件。
     */
    private List<AttachmentEntity> saveAttachments(Long messageId, List<MultipartFile> files) {
        List<AttachmentEntity> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(files)) {
            return list;
        }
        try {
            /* 1. 按顺序写入物理文件与数据库记录 */
            List<StoredFile> storedFiles = fileStorageService.storeFiles(files);
            for (StoredFile stored : storedFiles) {
                AttachmentEntity entity = attachmentManager.createAttachment(messageId, stored.getOriginalName(), stored.getMimeType(), stored.getStoredName(), stored.getSize());
                list.add(entity);
            }
        } catch (IOException ex) {
            throw new ServiceException(500, "保存附件失败");
        }
        return list;
    }

    /**
     * 1. 构建附件 Map。
     */
    private Map<Long, List<AttachmentEntity>> buildAttachmentMap(List<AttachmentEntity> attachments) {
        Map<Long, List<AttachmentEntity>> map = new HashMap<>();
        for (AttachmentEntity entity : attachments) {
            /* 1. 分组收集附件列表 */
            List<AttachmentEntity> list = map.getOrDefault(entity.getMessageId(), new ArrayList<>());
            list.add(entity);
            map.put(entity.getMessageId(), list);
        }
        return map;
    }

    /**
     * 1. 构建历史 VO。
     */
    private HistoryMessageVO buildHistoryVO(MessageEntity entity, List<AttachmentEntity> attachments) {
        HistoryMessageVO vo = messageConverter.toHistoryVO(entity);
        List<AttachmentVO> attachmentVOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(attachments)) {
            /* 1. 构建附件下载地址 */
            for (AttachmentEntity att : attachments) {
                AttachmentVO item = attachmentConverter.toVO(att);
                String url = "/agent/uploads/" + att.getStoredName();
                item.setUrl(url);
                attachmentVOS.add(item);
            }
        }
        vo.setAttachments(attachmentVOS);
        return vo;
    }

    /**
     * 1. 反转列表，保持时间顺序。
     */
    private List<MessageEntity> reverseList(List<MessageEntity> messages) {
        List<MessageEntity> copy = new ArrayList<>(messages);
        /* 1. 按 ID 正序排列，确保上下文顺序正确 */
        copy.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return copy;
    }
}
