package com.atun.brain.agent.rag.qdrant;

import com.atun.brain.agent.rag.spi.EmbeddingStoreProvider;
import com.atun.brain.agent.rag.spi.RetrievalService;
import com.atun.brain.agent.core.exception.RagRetrievalException;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于 Qdrant 的 RAG 检索服务实现
 * <p>
 * 封装向量化 + 搜索的完整流程，通过 EmbeddingStoreProvider SPI 解耦向量存储。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class QdrantRetrievalService implements RetrievalService {

    private final EmbeddingStoreProvider embeddingStoreProvider;
    private final EmbeddingModel embeddingModel;

    public QdrantRetrievalService(EmbeddingStoreProvider embeddingStoreProvider,
                                   EmbeddingModel embeddingModel) {
        this.embeddingStoreProvider = embeddingStoreProvider;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public EmbeddingSearchResult<TextSegment> retrieve(String query, int maxResults, Map<String, Object> filter) {
        log.debug("RAG 检索: query={}, maxResults={}", truncate(query, 50), maxResults);

        try {
            // 1. 向量化查询文本
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // 2. 执行向量搜索
            return embeddingStoreProvider.search(queryEmbedding, maxResults, 0.6);

        } catch (Exception e) {
            log.error("RAG 检索失败: query={}", truncate(query, 50), e);
            throw new RagRetrievalException("向量检索失败", e);
        }
    }

    @Override
    public String ingest(String text, Map<String, Object> metadata) {
        log.debug("RAG 入库: text={}", truncate(text, 50));

        try {
            // 1. 向量化
            Embedding embedding = embeddingModel.embed(text).content();

            // 2. 构建 TextSegment（含元数据）
            Map<String, Object> metaMap = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            metaMap.put("timestamp", System.currentTimeMillis());

            Metadata langchainMeta = Metadata.from(metaMap);
            TextSegment segment = TextSegment.from(text, langchainMeta);

            // 3. 存储
            return embeddingStoreProvider.store(embedding, segment);

        } catch (Exception e) {
            log.error("RAG 入库失败: text={}", truncate(text, 50), e);
            throw new RagRetrievalException("向量入库失败", e);
        }
    }

    @Override
    public void removeKnowledge(String id) {
        try {
            embeddingStoreProvider.removeById(id);
        } catch (Exception e) {
            log.error("RAG 删除失败: id={}", id, e);
            throw new RagRetrievalException("向量删除失败", e);
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
