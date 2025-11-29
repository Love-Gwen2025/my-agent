package com.ynp.agent.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.ynp.agent.domain.entity.AttachmentEntity;
import com.ynp.agent.domain.entity.MessageEntity;
import com.ynp.agent.mapper.AttachmentMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 附件数据访问。
 */
@Service
public class AttachmentManager extends BaseManager {

    private final AttachmentMapper attachmentMapper;

    public AttachmentManager(AttachmentMapper attachmentMapper) {
        this.attachmentMapper = attachmentMapper;
    }

    /**
     * 1. 根据消息批量查询附件。
     */
    public List<AttachmentEntity> listByMessageIds(List<Long> messageIds) {
        if (Objects.isNull(messageIds) || messageIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<AttachmentEntity> wrapper = new LambdaQueryWrapper<>();
        /* 1. 通过消息 ID 集合查询附件 */
        wrapper.in(AttachmentEntity::getMessageId, messageIds)
                .orderByAsc(AttachmentEntity::getId);
        return attachmentMapper.selectList(wrapper);
    }

    /**
     * 1. 按会话查询附件存储名，用于删除文件。
     */
    public List<String> listStoredNamesByConversation(Long conversationId) {
        MPJLambdaWrapper<AttachmentEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(AttachmentEntity::getStoredName)
                .leftJoin(MessageEntity.class, MessageEntity::getId, AttachmentEntity::getMessageId)
                .eq(MessageEntity::getConversationId, conversationId);
        List<AttachmentEntity> rows = attachmentMapper.selectJoinList(AttachmentEntity.class, wrapper);
        return rows.stream().map(AttachmentEntity::getStoredName).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 1. 创建附件记录。
     */
    public AttachmentEntity createAttachment(Long messageId, String fileName, String mimeType, String storedName, Long size) {
        AttachmentEntity entity = new AttachmentEntity();
        entity.setMessageId(messageId);
        entity.setFileName(fileName);
        entity.setMimeType(mimeType);
        entity.setStoredName(storedName);
        entity.setSize(size);
        /* 1. 插入附件记录 */
        attachmentMapper.insert(entity);
        return entity;
    }

    /**
     * 1. 删除会话下的附件记录。
     */
    public void deleteByConversation(Long conversationId) {
        MPJLambdaWrapper<AttachmentEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(AttachmentEntity::getId)
                .leftJoin(MessageEntity.class, MessageEntity::getId, AttachmentEntity::getMessageId)
                .eq(MessageEntity::getConversationId, conversationId);
        List<AttachmentEntity> rows = attachmentMapper.selectJoinList(AttachmentEntity.class, wrapper);
        List<Long> ids = rows.stream()
                .map(AttachmentEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        LambdaUpdateWrapper<AttachmentEntity> deleteWrapper = new LambdaUpdateWrapper<>();
        deleteWrapper.in(AttachmentEntity::getId, ids);
        /* 1. 批量删除会话下的附件记录 */
        attachmentMapper.delete(deleteWrapper);
    }
}
