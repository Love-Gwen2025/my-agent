package com.couple.agent.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 消息实体
 *
 * <p>用于持久化聊天消息内容、类型、状态等信息，支持后续历史查询与撤回操作</p>
 *
 * @author ynp
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
     * 引用的消息 ID，实现"回复某条消息"的功能
     */
    @TableField("reply_to")
    private Long replyTo;

    /**
     * 消息状态：1=正常，2=撤回，3=删除
     * 撤回或删除后仍保留记录，满足追溯需求
     */
    @TableField("status")
    private Integer status;

    /**
     * 消息发送时间，默认等于 createTime，可在特定场景下自定义写入
     */
    @TableField("send_time")
    private LocalDateTime sendTime;

    /**
     * 消息编辑时间，支持后续"修改消息"功能时记录最新更新时间
     */
    @TableField("edit_time")
    private LocalDateTime editTime;

    /**
     * 扩展字段（JSON 字符串），用于存放地理位置、引用卡片等附加信息
     */
    @TableField("ext")
    private String ext;
}
