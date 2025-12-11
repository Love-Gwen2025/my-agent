package com.couple.agent.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 嵌入模型配置。
 * 提供 EmbeddingModel Bean，供向量检索与入库组件复用。
 */
@Configuration
public class EmbeddingModelConfig {

    /**
     * 提供本地 ONNX 嵌入模型 Bean。
     *
     * @return 嵌入模型实例
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        // 1. 初始化 all-MiniLM-L6-v2 本地模型，用于生成向量
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
