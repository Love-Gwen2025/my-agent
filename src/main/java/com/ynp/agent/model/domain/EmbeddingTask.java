package com.ynp.agent.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 向量化任务实体
 *
 * <p>存储待处理的向量化任务，支持异步处理和失败重试</p>
 *
 * @author ynp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_embedding_task")
public class EmbeddingTask {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息ID
     */
    @TableField("message_id")
    private Long messageId;

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 待向量化的文本内容
     */
    @TableField("content_text")
    private String contentText;

    /**
     * 任务状态
     * 0=待处理，1=处理中，2=已完成，3=失败
     */
    @TableField("status")
    private Integer status;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 处理时间
     */
    @TableField("process_time")
    private LocalDateTime processTime;

    /**
     * 任务状态常量
     */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_FAILED = 3;
}
