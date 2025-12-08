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
 * 会话实体
 * 该类用于存储单聊、群聊或机器人会话的基础信息，所有字段均继承自 BasePo 并补充业务属性。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("t_conversation")
public class Conversation extends BasePo {

    /**
     * 会话类型：1=单聊，2=群聊，3=机器人。
     * 通过类型可以快速区分消息处理逻辑以及前端展示策略。
     */
    @TableField("type")
    private Integer type;

    /**
     * 会话标题或群聊名称，单聊时通常为空或由前端根据成员信息拼装。
     */
    @TableField("title")
    private String title;

    /**
     * 会话头像地址，群聊或机器人场景用于统一展示。
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 最近一条消息的 ID，便于快速获取预览内容与排序。
     */
    @TableField("last_message_id")
    private Long lastMessageId;

    /**
     * 最近一条消息的时间，配合 lastMessageId 可实现按活跃度排序。
     */
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 额外扩展信息（JSON 字符串），用于存储公告、主题色、群备注等非结构化配置。
     */
    @TableField("ext")
    private String ext;
}
