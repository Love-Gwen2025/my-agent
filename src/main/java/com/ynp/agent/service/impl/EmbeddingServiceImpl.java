package com.ynp.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ynp.agent.mangaer.BaseManager;
import com.ynp.agent.model.domain.EmbeddingTask;
import com.ynp.agent.model.domain.MessageEmbedding;
import com.ynp.agent.service.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 向量嵌入服务实现
 *
 * <p>使用 all-MiniLM-L6-v2 本地模型进行文本向量化</p>
 * <p>向量维度：384</p>
 *
 * @author ynp
 */
@Slf4j
@Service
public class EmbeddingServiceImpl extends BaseManager implements EmbeddingService {

    /**
     * 本地 Embedding 模型
     */
    private EmbeddingModel embeddingModel;

    /**
     * 初始化 Embedding 模型
     */
    @PostConstruct
    public void init() {
        log.info("初始化本地 Embedding 模型 (all-MiniLM-L6-v2)...");
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        log.info("Embedding 模型初始化完成，向量维度: 384");
    }

    /**
     * 将文本转换为向量
     *
     * @param text 待转换的文本
     * @return 向量数组
     */
    @Override
    public float[] embed(String text) {
        if (Objects.isNull(text) || text.isEmpty()) {
            return new float[384];
        }

        Embedding embedding = embeddingModel.embed(text).content();
        return embedding.vector();
    }

    /**
     * 批量将文本转换为向量
     *
     * @param texts 待转换的文本列表
     * @return 向量数组列表
     */
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (Objects.isNull(texts) || texts.isEmpty()) {
            return new ArrayList<>();
        }

        return texts.stream()
                .map(this::embed)
                .collect(Collectors.toList());
    }

    /**
     * 存储消息的向量嵌入
     *
     * @param messageId 消息ID
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param content 消息内容
     */
    @Override
    public void storeMessageEmbedding(Long messageId, Long conversationId, Long userId, String content) {
        if (Objects.isNull(content) || content.isEmpty()) {
            log.debug("消息内容为空，跳过向量化，messageId: {}", messageId);
            return;
        }

        try {
            // 1. 生成向量
            float[] vector = embed(content);

            // 2. 转换为字符串格式（pgvector 格式）
            String embeddingStr = vectorToString(vector);

            // 3. 存储到数据库
            messageEmbeddingMapper.insertEmbedding(
                    messageId,
                    conversationId,
                    userId,
                    content,
                    embeddingStr
            );

            log.debug("消息向量化完成，messageId: {}", messageId);

        } catch (Exception e) {
            log.error("消息向量化失败，messageId: {}", messageId, e);
            throw new RuntimeException("向量化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 语义搜索相关记忆
     *
     * @param conversationId 会话ID
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 相关消息内容列表
     */
    @Override
    public List<String> searchRelevantMemories(Long conversationId, String query, int topK) {
        if (Objects.isNull(query) || query.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 1. 生成查询向量
            float[] queryVector = embed(query);
            String embeddingStr = vectorToString(queryVector);

            // 2. 执行相似度搜索
            List<MessageEmbedding> results = messageEmbeddingMapper.searchSimilar(
                    conversationId, embeddingStr, topK);

            // 3. 提取内容
            return results.stream()
                    .map(MessageEmbedding::getContentText)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("语义搜索失败，conversationId: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 在用户范围内语义搜索相关记忆
     *
     * @param userId 用户ID
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 相关消息内容列表
     */
    @Override
    public List<String> searchRelevantMemoriesByUser(Long userId, String query, int topK) {
        if (Objects.isNull(query) || query.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 1. 生成查询向量
            float[] queryVector = embed(query);
            String embeddingStr = vectorToString(queryVector);

            // 2. 执行相似度搜索
            List<MessageEmbedding> results = messageEmbeddingMapper.searchSimilarByUser(
                    userId, embeddingStr, topK);

            // 3. 提取内容
            return results.stream()
                    .map(MessageEmbedding::getContentText)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("用户范围语义搜索失败，userId: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 创建向量化任务（异步处理）
     *
     * @param messageId 消息ID
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param content 消息内容
     */
    @Override
    public void createEmbeddingTask(Long messageId, Long conversationId, Long userId, String content) {
        if (Objects.isNull(content) || content.isEmpty()) {
            return;
        }

        EmbeddingTask task = EmbeddingTask.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .userId(userId)
                .contentText(content)
                .status(EmbeddingTask.STATUS_PENDING)
                .retryCount(0)
                .createTime(LocalDateTime.now())
                .build();

        embeddingTaskMapper.insert(task);
        log.debug("创建向量化任务，messageId: {}, taskId: {}", messageId, task.getId());
    }

    /**
     * 将 float 数组转换为 pgvector 格式字符串
     *
     * @param vector 向量数组
     * @return pgvector 格式字符串，如 "[0.1,0.2,0.3]"
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
