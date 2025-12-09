package com.ynp.agent.service;

import java.util.List;

/**
 * 向量嵌入服务接口
 *
 * <p>提供文本向量化和语义搜索能力</p>
 *
 * @author ynp
 */
public interface EmbeddingService {

    /**
     * 将文本转换为向量
     *
     * @param text 待转换的文本
     * @return 向量数组
     */
    float[] embed(String text);

    /**
     * 批量将文本转换为向量
     *
     * @param texts 待转换的文本列表
     * @return 向量数组列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 存储消息的向量嵌入
     *
     * @param messageId 消息ID
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param content 消息内容
     */
    void storeMessageEmbedding(Long messageId, Long conversationId, Long userId, String content);

    /**
     * 语义搜索相关记忆
     *
     * <p>在指定会话中搜索与查询语义相似的历史消息</p>
     *
     * @param conversationId 会话ID
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 相关消息内容列表
     */
    List<String> searchRelevantMemories(Long conversationId, String query, int topK);

    /**
     * 在用户范围内语义搜索相关记忆
     *
     * @param userId 用户ID
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 相关消息内容列表
     */
    List<String> searchRelevantMemoriesByUser(Long userId, String query, int topK);

    /**
     * 创建向量化任务（异步处理）
     *
     * @param messageId 消息ID
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param content 消息内容
     */
    void createEmbeddingTask(Long messageId, Long conversationId, Long userId, String content);
}
