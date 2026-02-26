package com.atun.brain.agent.config;

import com.atun.brain.agent.rag.qdrant.QdrantEmbeddingStoreProvider;
import com.atun.brain.agent.rag.qdrant.QdrantRetrievalService;
import com.atun.brain.agent.rag.spi.EmbeddingStoreProvider;
import com.atun.brain.agent.rag.spi.RetrievalService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 层自动配置
 * <p>
 * 条件化注册 EmbeddingStoreProvider / RetrievalService Bean。
 * 默认使用 Qdrant，可通过更换 starter 依赖切换向量库。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "agent.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentRagAutoConfiguration {
    
    private final AgentProperties agentProperties;
    
    /**
     * Qdrant 客户端
     */
    @Bean
    @ConditionalOnMissingBean(QdrantClient.class)
    @ConditionalOnClass(QdrantClient.class)
    @ConditionalOnProperty(prefix = "agent.rag", name = "store", havingValue = "qdrant", matchIfMissing = true)
    public QdrantClient qdrantClient() {
        AgentProperties.QdrantConfig qdrantConfig = agentProperties.getRag().getQdrant();
        
        log.info("初始化 Qdrant 客户端: host={}, port={}", qdrantConfig.getHost(), qdrantConfig.getPort());
        
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(
                        qdrantConfig.getHost(),
                        qdrantConfig.getPort(),
                        qdrantConfig.isUseTls()
                ).build()
        );
    }
    
    /**
     * Qdrant EmbeddingStoreProvider
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingStoreProvider.class)
    @ConditionalOnClass(QdrantClient.class)
    @ConditionalOnProperty(prefix = "agent.rag", name = "store", havingValue = "qdrant", matchIfMissing = true)
    public EmbeddingStoreProvider qdrantEmbeddingStoreProvider(QdrantClient qdrantClient) {
        AgentProperties.QdrantConfig config = agentProperties.getRag().getQdrant();
        
        QdrantEmbeddingStoreProvider provider = new QdrantEmbeddingStoreProvider(
                qdrantClient,
                config.getCollectionName(),
                config.getVectorSize()
        );
        
        // 初始化集合
        provider.initialize(config.getCollectionName(), config.getVectorSize());
        
        log.info("初始化 Qdrant EmbeddingStoreProvider: collection={}, vectorSize={}",
                config.getCollectionName(), config.getVectorSize());
        
        return provider;
    }
    
    /**
     * RAG 检索服务
     */
    @Bean
    @ConditionalOnMissingBean(RetrievalService.class)
    public RetrievalService retrievalService(EmbeddingStoreProvider embeddingStoreProvider,
                                              EmbeddingModel embeddingModel) {
        log.info("初始化 RAG 检索服务: provider={}", embeddingStoreProvider.getProviderName());
        return new QdrantRetrievalService(embeddingStoreProvider, embeddingModel);
    }
}
