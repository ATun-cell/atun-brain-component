package com.atun.brain.agent.core.pipeline;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.model.AgentResponse;

/**
 * Pipeline 第四阶段：记忆持久化器
 * <p>
 * 同步更新短期记忆窗口，异步触发 ApplicationEvent
 * 通知 EmbeddingStore 将关键对话摘要向量化入库。
 *
 * @author lij
 * @since 1.0
 */
public interface MemoryPersister {
    
    /**
     * 持久化对话记忆
     *
     * @param request  原始请求
     * @param response 最终响应
     */
    void persist(AgentRequest request, AgentResponse response);
}
