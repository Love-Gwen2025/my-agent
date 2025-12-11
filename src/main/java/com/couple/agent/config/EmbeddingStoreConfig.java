package com.couple.agent.config;


import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class EmbeddingStoreConfig {

    @Autowired
    EmbeddingModel embeddingModel;

    @Autowired
    DataSource dataSource;


    @Bean
    public EmbeddingStore<TextSegment> userMessagePgVectorEembeddingStore(){

        /*
         1. 直接使用默认元数据存储模式（COMBINED_JSON），无需额外配置。
         2. 创建基于数据源的 PgVectorEmbeddingStore，开启向量索引。
         */
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .dimension(embeddingModel.dimension())
                .useIndex(true)
                .indexListSize(100)
                .createTable(true)
                .table("t_message_embedding")
                .build();
    }
}
