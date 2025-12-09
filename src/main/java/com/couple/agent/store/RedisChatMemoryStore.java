package com.couple.agent.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 基于 Redis 的聊天记忆存储实现
 *
 * <p>使用 Redis 作为聊天记忆的缓存存储，支持：</p>
 * <ul>
 *     <li>快速读写聊天记录</li>
 *     <li>自动过期清理（默认24小时）</li>
 *     <li>JSON 格式序列化</li>
 * </ul>
 *
 * @author ynp
 */
@Slf4j
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    /**
     * Redis 键前缀
     */
    private static final String MEMORY_KEY_PREFIX = "chat:memory:";

    /**
     * 缓存过期时间（24小时）
     */
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取指定会话的聊天消息列表
     *
     * @param memoryId 会话记忆ID（通常为会话ID）
     * @return 聊天消息列表，如果不存在则返回空列表
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = buildKey(memoryId);
        String json = redisTemplate.opsForValue().get(key);

        // 检查缓存是否存在
        if (!StringUtils.hasText(json)) {
            log.debug("缓存未命中，memoryId: {}", memoryId);
            return new ArrayList<>();
        }

        log.debug("从Redis加载聊天记忆，memoryId: {}", memoryId);
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    /**
     * 更新指定会话的聊天消息列表
     *
     * @param memoryId 会话记忆ID
     * @param messages 新的消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        if (Objects.isNull(messages) || messages.isEmpty()) {
            log.debug("消息列表为空，跳过更新，memoryId: {}", memoryId);
            return;
        }

        String key = buildKey(memoryId);
        String json = ChatMessageSerializer.messagesToJson(messages);

        // 写入 Redis 并设置过期时间
        redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        log.debug("更新Redis聊天记忆，memoryId: {}, 消息数: {}", memoryId, messages.size());
    }

    /**
     * 删除指定会话的聊天消息
     *
     * @param memoryId 会话记忆ID
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String key = buildKey(memoryId);
        Boolean deleted = redisTemplate.delete(key);
        log.debug("删除Redis聊天记忆，memoryId: {}, 结果: {}", memoryId, deleted);
    }

    /**
     * 刷新缓存过期时间
     *
     * @param memoryId 会话记忆ID
     */
    public void refreshExpire(Object memoryId) {
        String key = buildKey(memoryId);
        redisTemplate.expire(key, CACHE_TTL);
    }

    /**
     * 构建 Redis 键
     *
     * @param memoryId 会话记忆ID
     * @return 完整的 Redis 键
     */
    private String buildKey(Object memoryId) {
        return MEMORY_KEY_PREFIX + memoryId;
    }
}
