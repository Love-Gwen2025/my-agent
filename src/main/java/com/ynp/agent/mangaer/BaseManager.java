package com.ynp.agent.mangaer;

import com.ynp.agent.mapper.AiModelMapper;
import com.ynp.agent.mapper.ConversationMapper;
import com.ynp.agent.mapper.ConversationMemberMapper;
import com.ynp.agent.mapper.EmbeddingTaskMapper;
import com.ynp.agent.mapper.MessageEmbeddingMapper;
import com.ynp.agent.mapper.MessageMapper;
import com.ynp.agent.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Manager 基类
 *
 * <p>集中管理 Mapper 层的依赖注入</p>
 *
 * @author ynp
 */
public class BaseManager {

    /**
     * 用户 Mapper
     */
    @Autowired
    protected UserMapper userMapper;

    /**
     * 会话 Mapper
     */
    @Autowired
    protected ConversationMapper conversationMapper;

    /**
     * 会话成员 Mapper
     */
    @Autowired
    protected ConversationMemberMapper conversationMemberMapper;

    /**
     * 消息 Mapper
     */
    @Autowired
    protected MessageMapper messageMapper;

    /**
     * AI 模型 Mapper
     */
    @Autowired
    protected AiModelMapper aiModelMapper;

    /**
     * 消息向量嵌入 Mapper
     */
    @Autowired
    protected MessageEmbeddingMapper messageEmbeddingMapper;

    /**
     * 向量化任务 Mapper
     */
    @Autowired
    protected EmbeddingTaskMapper embeddingTaskMapper;
}
