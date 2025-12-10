package com.couple.agent.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.couple.agent.model.param.ConversationParam;
import com.couple.agent.model.vo.ConversationVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 会话实体
 *
 * 该类用于存储单聊、群聊或 AI 对话会话的基础信息
 *
 * @author ynp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("t_conversation")
public class Conversation extends BasePo {

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话标题或群聊名称
     * 单聊时通常为空或由前端根据成员信息拼装
     */
    @TableField("title")
    private String title;

    /**
     * 使用的 AI 模型编码
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 会话头像地址
     * 群聊或机器人场景用于统一展示
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 最近一条消息的 ID
     * 便于快速获取预览内容与排序
     */
    @TableField("last_message_id")
    private Long lastMessageId;

    /**
     * 最近一条消息的时间
     * 配合 lastMessageId 可实现按活跃度排序
     */
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 额外扩展信息（JSON 字符串）
     * 用于存储公告、主题色、群备注等非结构化配置
     */
    @TableField("ext")
    private String ext;

    public static ConversationVo toConversationVo(Conversation conversation) {
        if (Objects.isNull(conversation)) {
            return null;
        }
        LocalDateTime lastMessageAt = Objects.nonNull(conversation.getLastMessageAt())
                ? conversation.getLastMessageAt()
                : conversation.getUpdateTime();
        return ConversationVo.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createTime(conversation.getCreateTime())
                .lastMessageAt(lastMessageAt)
                .build();
    }

    public static Conversation ofParam(ConversationParam conversationParam) {
        if (Objects.isNull(conversationParam)) {
            return null;
        }
        return Conversation.builder()
                .id(conversationParam.getId())
                .title(conversationParam.getTitle())
                .build();
    }
}
