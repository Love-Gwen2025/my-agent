package com.ynp.agent.service.ai;

import com.ynp.agent.config.AppProperties;
import com.ynp.agent.domain.document.AiCacheDocument;
import com.ynp.agent.domain.entity.AttachmentEntity;
import com.ynp.agent.domain.entity.MessageEntity;
import com.ynp.agent.exception.ServiceException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AI 调用服务，使用 Spring AI 统一封装。
 */
@Service
public class AiService {

    private final OpenAiChatModel openAiChatModel;
    private final AppProperties appProperties;
    private final MongoTemplate mongoTemplate;

    public AiService(OpenAiChatModel openAiChatModel, AppProperties appProperties, MongoTemplate mongoTemplate) {
        this.openAiChatModel = openAiChatModel;
        this.appProperties = appProperties;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 1. 将历史消息转换为模型可读的 Message 序列。
     */
    private List<Message> buildMessages(List<MessageEntity> history, Map<Long, List<AttachmentEntity>> attachmentMap) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(appProperties.getSystemPrompt())) {
            messages.add(new SystemMessage(appProperties.getSystemPrompt()));
        }
        for (MessageEntity item : history) {
            String content = item.getContent();
            List<AttachmentEntity> attaches = attachmentMap.getOrDefault(item.getId(), new ArrayList<>());
            if (!CollectionUtils.isEmpty(attaches)) {
                List<String> noteLines = new ArrayList<>();
                int index = 1;
                for (AttachmentEntity att : attaches) {
                    String line = index + ". 文件名：" + att.getFileName() + "，类型：" + att.getMimeType() + "，大小：" + buildReadableSize(att.getSize());
                    noteLines.add(line);
                    index = index + 1;
                }
                content = content + "\n\n[附件说明]\n" + String.join("\n", noteLines);
            }
            if ("assistant".equalsIgnoreCase(item.getRole())) {
                messages.add(new AssistantMessage(content));
            } else {
                messages.add(new UserMessage(content));
            }
        }
        return messages;
    }

    /**
     * 1. 组装 Prompt 并调用模型获取回复。
     */
    public String chat(Long userId, Long conversationId, List<MessageEntity> history, Map<Long, List<AttachmentEntity>> attachmentMap) {
        if (!StringUtils.hasText(appProperties.getGptApiKey()) || !StringUtils.hasText(appProperties.getGptEndpoint())) {
            throw new ServiceException(500, "尚未配置 GPT 接口信息");
        }
        /* 1. 组装模型输入消息 */
        List<Message> messages = buildMessages(history, attachmentMap);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(appProperties.getGptModel())
                .withTemperature(appProperties.getGptTemperature())
                .build();
        Prompt prompt = new Prompt(messages, options);
        /* 2. 调用 Spring AI 模型 */
        ChatResponse response = openAiChatModel.call(prompt);
        if (Objects.isNull(response) || Objects.isNull(response.getResult()) || Objects.isNull(response.getResult().getOutput())) {
            throw new ServiceException(500, "无效的 GPT 响应");
        }
        String reply = response.getResult().getOutput().getContent();
        if (!StringUtils.hasText(reply)) {
            throw new ServiceException(500, "无效的 GPT 响应");
        }
        /* 3. 写入 MongoDB 缓存上下文 */
        cacheContext(userId, conversationId, messages, reply);
        return reply.trim();
    }

    /**
     * 1. 缓存上下文到 MongoDB，方便后续读取。
     */
    private void cacheContext(Long userId, Long conversationId, List<Message> messages, String reply) {
        List<String> merged = messages.stream()
                .map(Message::getContent)
                .collect(Collectors.toCollection(ArrayList::new));
        merged.add(reply);
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is(conversationId));
        AiCacheDocument doc = mongoTemplate.findOne(query, AiCacheDocument.class);
        if (Objects.isNull(doc)) {
            doc = new AiCacheDocument();
        }
        doc.setConversationId(conversationId);
        doc.setUserId(userId);
        doc.setMessages(merged);
        doc.setUpdatedAt(Instant.now());
        mongoTemplate.save(doc);
    }

    /**
     * 1. 构造可读大小字符串。
     */
    private String buildReadableSize(Long size) {
        if (Objects.isNull(size)) {
            return "0 KB";
        }
        double kb = size / 1024.0;
        return String.format("%.1f KB", kb);
    }
}
