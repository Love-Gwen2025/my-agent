package com.couple.agent.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 消息向量嵌入实体
 * 存储消息的向量表示，用于长期记忆的语义搜索
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("t_message_embedding")
public class MessageEmbedding extends BasePo {

    /**
     * 关联的消息ID
     */
    @TableField("message_id")
    private Long messageId;

    /**
     * 所属会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 原始文本内容
     */
    @TableField("content_text")
    private String contentText;

    /**
     * 向量嵌入数据（存储为字符串格式）
     * 实际使用 pgvector 的 vector 类型
     */
    @TableField("embedding")
    private String embedding;
}
