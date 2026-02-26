package com.atun.brain.agent.core.pipeline;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.model.AgentResponse;

/**
 * Pipeline 第三阶段：响应合成器
 * <p>
 * 融合 LLM 输出与工具结果，应用 RAG 增强。
 * 如果 RAG 模块可用，将检索相关上下文并附加到响应中。
 *
 * @author lij
 * @since 1.0
 */
public interface ResponseComposer {
    
    /**
     * 合成最终响应
     *
     * @param request          原始请求
     * @param orchestratorResponse 工具编排器输出的初始响应
     * @return 经过 RAG 增强后的最终响应
     */
    AgentResponse compose(AgentRequest request, AgentResponse orchestratorResponse);
}
