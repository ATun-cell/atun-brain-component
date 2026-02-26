package com.atun.brain.agent.core.pipeline.impl;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.model.AgentResponse;
import com.atun.brain.agent.core.pipeline.ResponseComposer;
import com.atun.brain.agent.rag.spi.RetrievalService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 默认响应合成器实现
 * <p>
 * 当 RAG 模块可用时，检索相关上下文片段并附加到响应中。
 * 当前版本为直通模式（pass-through），RAG 结果不再覆盖 LLM 输出，
 * 而是作为补充信息附加。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class DefaultResponseComposer implements ResponseComposer {
    
    private final RetrievalService retrievalService;
    
    /**
     * @param retrievalService RAG 检索服务（可选，为 null 时跳过 RAG 增强）
     */
    public DefaultResponseComposer(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }
    
    @Override
    public AgentResponse compose(AgentRequest request, AgentResponse orchestratorResponse) {
        // 如果 RAG 服务不可用或编排器已失败，直接返回
        if (retrievalService == null || !orchestratorResponse.message().startsWith("抱歉")) {
            return orchestratorResponse;
        }
        
        try {
            // 检索相关上下文
            EmbeddingSearchResult<TextSegment> searchResult = retrievalService.retrieve(
                    request.getUserMessage(),
                    3, // 最多返回 3 条相关片段
                    Map.of("userId", request.getUserId())
            );
            
            if (searchResult.matches().isEmpty()) {
                return orchestratorResponse;
            }
            
            // 构建 RAG 补充信息
            StringBuilder ragContext = new StringBuilder();
            ragContext.append("\n\n📚 相关历史记录：\n");
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                if (match.score() >= 0.7) {
                    ragContext.append("- ").append(match.embedded().text()).append("\n");
                }
            }
            
            if (ragContext.length() > "📚 相关历史记录：\n".length() + 2) {
                log.info("[ResponseComposer] requestId={} 附加 {} 条 RAG 上下文",
                        request.getRequestId(), searchResult.matches().size());
                
                return new AgentResponse(
                        orchestratorResponse.requestId(),
                        orchestratorResponse.message() + ragContext,
                        orchestratorResponse.hasToolCalls(),
                        orchestratorResponse.toolsUsed(),
                        true, // RAG applied
                        orchestratorResponse.inputTokens(),
                        orchestratorResponse.outputTokens(),
                        orchestratorResponse.latencyMs(),
                        orchestratorResponse.timestamp()
                );
            }
            
        } catch (Exception e) {
            log.warn("[ResponseComposer] RAG 检索失败，降级为直通模式", e);
        }
        
        return orchestratorResponse;
    }
}
