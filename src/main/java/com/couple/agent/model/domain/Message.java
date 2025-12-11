package com.couple.agent.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.couple.agent.model.vo.HistoryMessageVo;
import com.couple.agent.model.vo.MessageVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

/**
 * 消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("t_message")
public class Message extends BasePo {

    /**
     * 会话 ID，消息归属的会话标识
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 发送者用户 ID，用于关联用户信息并做权限校验
     * -1 表示 AI 助手发送
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 角色：user / assistant / system
     */
    @TableField("role")
    private String role;

    /**
     * 消息正文内容
     * 文本直接保存，结构化内容（如卡片、音视频）可序列化为 JSON
     */
    @TableField("content")
    private String content;

    /**
     * 消息类型：TEXT/IMAGE/FILE/AUDIO 等
     * 驱动前端的展示方式与解析逻辑
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 父消息的id
     * */
    @TableField("parent_id")
    private Long parentId;

    /**
     * Token 数量（AI 回复时记录）
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * 使用的模型编码（AI 回复时记录）
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 消息状态：1=正常，2=撤回，3=删除
     * 撤回或删除后仍保留记录，满足追溯需求
     */
    @TableField("status")
    private Integer status;

    /**
     * 扩展字段（JSON 字符串），用于存放地理位置、引用卡片等附加信息
     */
    @TableField("ext")
    private String ext;

    public static MessageVo toMessageVo(Message message) {
        MessageVo messageVo =new MessageVo();
        BeanUtils.copyProperties(message,messageVo);
        return messageVo;
    }
}
