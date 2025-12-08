package com.ynp.agent.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 消息附件实体
 * 记录图片、文件、语音等非文本内容的存储信息，方便在不同终端下载和展示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("t_message_attachment")
public class MessageAttachment extends BasePo {

    /**
     * 消息 ID，指向主消息表的主键。
     */
    @TableField("message_id")
    private Long messageId;

    /**
     * 附件类型：IMAGE/FILE/VOICE 等，前端据此选择渲染方式。
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 文件原始名称，供用户识别附件。
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件大小（字节），前端可据此展示或做上传限制。
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 附件访问地址，可对接对象存储或 CDN。
     */
    @TableField("url")
    private String url;

    /**
     * 额外信息（JSON 字符串），如图片宽高、音视频时长、缩略图等。
     */
    @TableField("extra")
    private String extra;
}
