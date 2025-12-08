package com.ynp.agent.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 消息回执实体
 * 用于记录消息在不同接收者处的送达/已读状态，为未读数和回执展示提供数据支撑。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("t_message_receipt")
public class MessageReceipt extends BasePo {

    /**
     * 消息 ID，关联主消息表。
     */
    @TableField("message_id")
    private Long messageId;

    /**
     * 会话 ID，冗余字段，用于快速按会话维度查询回执信息。
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 接收者用户 ID。
     */
    @TableField("receiver_id")
    private Long receiverId;

    /**
     * 回执状态：0=未读，1=送达，2=已读。
     */
    @TableField("status")
    private Integer status;

    /**
     * 消息被标记为已读的时间，用于呈现阅读时间线。
     */
    @TableField("read_time")
    private LocalDateTime readTime;
}
