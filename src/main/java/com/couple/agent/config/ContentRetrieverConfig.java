package com.couple.agent.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContentRetrieverConfig {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> userMessagePgVectorEembeddingStore;

    @Bean
    /*
    * 检索器
    * */
    public ContentRetriever userMessageContentRetriever(){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(userMessagePgVectorEembeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                // maxResults 也可以根据查询动态指定(这里写死为3，动态高于静态，下同)
                .dynamicMaxResults(query -> 3)
                .minScore(0.75)
                // minScore 也可以根据查询动态指定（这里写死为0.75）
                .dynamicMinScore(query -> 0.75)
                // filter 也可以根据查询动态指定
                .build();
    }
}
