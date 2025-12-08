package com.ynp.agent.mangaer;


import com.ynp.agent.model.domain.Conversation;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationManager {

    /**
     * 新增会话记录。
     *
     * @param conversation 会话实体
     * @return 会话ID
     */
    Long insertConversation(Conversation conversation);

    /**
     * 为指定会话批量新增成员。
     *
     * @param conversationId 会话ID
     * @param memberIds      成员ID列表
     */
    void insertMembers(Long conversationId, List<Long> memberIds);

    /**
     * 查询会话详情。
     *
     * @param conversationId 会话ID
     * @return 会话实体
     */
    Conversation selectById(Long conversationId);

    /**
     * 判断用户是否在会话中。
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @return 是否为成员
     */
    boolean existsMember(Long conversationId, Long userId);

    /**
     * 更新会话的最近消息信息。
     *
     * @param conversationId 会话ID
     * @param messageId      最新消息ID
     * @param sendTime       最新消息时间
     */
    void updateLastMessage(Long conversationId, Long messageId, LocalDateTime sendTime);

    /**
     * 按照成员集合与会话类型查询会话。
     * <p>仅当会话的有效成员集合与入参完全一致时才返回，用于单聊等唯一性校验。</p>
     *
     * @param type      会话类型
     * @param memberIds 成员ID集合
     * @return 完全匹配的会话，若不存在返回 {@code null}
     */
    Conversation selectByMembersAndType(@NotNull(message = "会话类型不能为空") Integer type, List<Long> memberIds);

    /**
     * 查询会话中所有有效成员的用户ID。
     *
     * @param conversationId 会话ID
     * @return 有效成员ID列表，若会话不存在或无成员则返回空列表
     */
    List<Long> listActiveMemberIds(Long conversationId);

    int updateById(Conversation conversation);
}
