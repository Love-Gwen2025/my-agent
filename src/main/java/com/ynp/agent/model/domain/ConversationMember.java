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
 * 会话成员实体
 * 记录用户加入会话的关联关系，包含角色、禁言、未读游标等业务字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("t_conversation_member")
public class ConversationMember extends BasePo {

    /**
     * 会话 ID，对应 conversation.id，用于区分所属聊天。
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 成员用户 ID，关联系统用户表。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 成员角色：0=普通成员，1=管理员，2=群主。
     * 不同角色在群聊中拥有不同的管理权限。
     */
    @TableField("role")
    private Integer role;

    /**
     * 会话内昵称/备注名，仅对当前成员生效，方便家庭成员之间的个性化显示。
     */
    @TableField("alias")
    private String alias;

    /**
     * 加入会话时间，用于展示成员加入顺序或计算在线时长。
     */
    @TableField("join_time")
    private LocalDateTime joinTime;

    /**
     * 成员最后阅读的消息 ID，结合最新消息可快速得出未读数量。
     */
    @TableField("last_read_msg_id")
    private Long lastReadMsgId;

    /**
     * 是否被禁言：0=未禁言，1=已禁言。
     * 管理员可以根据需要限制成员发送消息。
     */
    @TableField("mute")
    private Integer mute;

    /**
     * 成员状态：1=正常在群，0=已退出或被移除。
     * 退出后仍保留记录，方便审计与历史追踪。
     */
    @TableField("status")
    private Integer status;

    /**
     * 扩展字段（JSON 字符串），用于存放个性化设置或后续功能的拓展信息。
     */
    @TableField("ext")
    private String ext;
}
