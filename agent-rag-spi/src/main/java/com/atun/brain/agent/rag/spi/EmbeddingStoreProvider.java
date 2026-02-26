package com.atun.brain.agent.rag.spi;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

import java.util.List;

/**
 * Embedding 存储提供者 SPI
 * <p>
 * 扩展 LangChain4j 的 EmbeddingStore 接口，增加集合管理和带过滤搜索能力。
 * 规范：EmbeddingStore 必须通过 EmbeddingStoreProvider 接口获取，禁止硬编码构造器。
 *
 * @author lij
 * @since 1.0
 */
public interface EmbeddingStoreProvider {
    
    /**
     * 获取底层 EmbeddingStore 实例
     * <p>
     * 返回的实例符合 LangChain4j EmbeddingStore&lt;TextSegment&gt; 接口，
     * 可直接用于 RetrievalAugmentor 配置。
     *
     * @return EmbeddingStore 实例
     */
    dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> getEmbeddingStore();
    
    /**
     * 存储文本段及其向量
     *
     * @param embedding   文本向量
     * @param textSegment 原始文本段（含元数据）
     * @return 存储后的向量ID
     */
    String store(Embedding embedding, TextSegment textSegment);
    
    /**
     * 批量存储
     *
     * @param embeddings   向量列表
     * @param textSegments 文本段列表
     * @return 向量ID列表
     */
    List<String> storeAll(List<Embedding> embeddings, List<TextSegment> textSegments);
    
    /**
     * 相似度搜索
     *
     * @param queryEmbedding 查询向量
     * @param maxResults     最大返回数量
     * @param minScore       最小相似度阈值（0-1）
     * @return 搜索结果
     */
    EmbeddingSearchResult<TextSegment> search(Embedding queryEmbedding, int maxResults, double minScore);
    
    /**
     * 根据 ID 删除向量
     *
     * @param id 向量ID
     */
    void removeById(String id);
    
    /**
     * 初始化存储（创建集合/索引等）
     *
     * @param collectionName 集合名称
     * @param dimensions     向量维度
     */
    void initialize(String collectionName, int dimensions);
    
    /**
     * 获取存储提供者名称
     *
     * @return 名称标识，如 "qdrant"、"milvus"、"pgvector"
     */
    String getProviderName();
}
