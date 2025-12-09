package com.ynp.agent.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ynp.agent.model.domain.MessageEmbedding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息向量嵌入 Mapper 接口
 *
 * <p>提供向量数据的存储和语义搜索功能</p>
 *
 * @author ynp
 */
@Mapper
public interface MessageEmbeddingMapper extends MPJBaseMapper<MessageEmbedding> {

    /**
     * 使用向量相似度搜索相关消息
     *
     * <p>基于 pgvector 的余弦相似度搜索</p>
     *
     * @param conversationId 会话ID
     * @param embedding 查询向量（字符串格式）
     * @param limit 返回结果数量限制
     * @return 相关消息嵌入列表
     */
    @Select("""
            SELECT id, message_id, conversation_id, user_id, content_text,
                   embedding::text as embedding, create_time
            FROM t_message_embedding
            WHERE conversation_id = #{conversationId}
            ORDER BY embedding <=> #{embedding}::vector
            LIMIT #{limit}
            """)
    List<MessageEmbedding> searchSimilar(
            @Param("conversationId") Long conversationId,
            @Param("embedding") String embedding,
            @Param("limit") int limit);

    /**
     * 使用向量相似度在用户范围内搜索相关消息
     *
     * @param userId 用户ID
     * @param embedding 查询向量
     * @param limit 返回结果数量限制
     * @return 相关消息嵌入列表
     */
    @Select("""
            SELECT id, message_id, conversation_id, user_id, content_text,
                   embedding::text as embedding, create_time
            FROM t_message_embedding
            WHERE user_id = #{userId}
            ORDER BY embedding <=> #{embedding}::vector
            LIMIT #{limit}
            """)
    List<MessageEmbedding> searchSimilarByUser(
            @Param("userId") Long userId,
            @Param("embedding") String embedding,
            @Param("limit") int limit);

    /**
     * 插入向量嵌入数据
     *
     * <p>使用原生 SQL 处理 pgvector 类型</p>
     *
     * @param messageId 消息ID
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param contentText 原始文本
     * @param embedding 向量数据（字符串格式）
     * @return 插入的记录数
     */
    @Select("""
            INSERT INTO t_message_embedding
            (message_id, conversation_id, user_id, content_text, embedding, create_time)
            VALUES (#{messageId}, #{conversationId}, #{userId}, #{contentText},
                    #{embedding}::vector, CURRENT_TIMESTAMP)
            RETURNING id
            """)
    Long insertEmbedding(
            @Param("messageId") Long messageId,
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("contentText") String contentText,
            @Param("embedding") String embedding);
}
