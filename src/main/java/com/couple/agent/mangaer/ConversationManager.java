package com.couple.agent.mangaer;


import com.couple.agent.model.domain.Conversation;
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
     * 查询会话详情。
     *
     * @param conversationId 会话ID
     * @return 会话实体
     */
    Conversation selectById(Long conversationId);

    /**
     * 更新会话的最近消息信息。
     *
     * @param conversationId 会话ID
     * @param messageId      最新消息ID
     * @param sendTime       最新消息时间
     */
    void updateLastMessage(Long conversationId, Long messageId, LocalDateTime sendTime);

    int updateById(Conversation conversation);

    /**
     * 查询用户参与的会话列表，按活跃时间倒序。
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Conversation> listByUser(Long userId);

    /**
     * 更新会话标题。
     *
     * @param conversationId 会话ID
     * @param title          新标题
     * @return 受影响行数
     */
    int updateTitle(Long conversationId, String title);

    /**
     * 删除会话并返回受影响行数。
     *
     * @param conversationId 会话ID
     * @return 受影响行数
     */
    int deleteConversation(Long conversationId);

    int updateConversation(Conversation conversation);
}
