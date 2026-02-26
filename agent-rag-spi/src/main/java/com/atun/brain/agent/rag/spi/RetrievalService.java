package com.atun.brain.agent.rag.spi;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

import java.util.Map;

/**
 * RAG 检索服务 SPI - 提供语义检索增强能力
 * <p>
 * 封装向量搜索 + 结果排序 + 上下文裁剪的完整 RAG 流程。
 *
 * @author lij
 * @since 1.0
 */
public interface RetrievalService {
    
    /**
     * 根据查询文本进行语义搜索
     *
     * @param query      查询文本
     * @param maxResults 最大返回数量
     * @param filter     过滤条件（如 userId）
     * @return 检索结果
     */
    EmbeddingSearchResult<TextSegment> retrieve(String query, int maxResults, Map<String, Object> filter);
    
    /**
     * 将文本存入向量知识库
     *
     * @param text     文本内容
     * @param metadata 元数据（如 userId、category 等）
     * @return 向量ID
     */
    String ingest(String text, Map<String, Object> metadata);
    
    /**
     * 根据向量ID删除知识
     *
     * @param id 向量ID
     */
    void removeKnowledge(String id);
}
