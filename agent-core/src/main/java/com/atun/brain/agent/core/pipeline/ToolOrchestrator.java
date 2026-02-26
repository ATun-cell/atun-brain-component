package com.atun.brain.agent.core.pipeline;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.model.AgentResponse;
import com.atun.brain.agent.tools.spi.ToolRouteDecision;

/**
 * Pipeline 第二阶段：工具编排器
 * <p>
 * 根据 IntentClassifier 的路由决策执行工具链或直连 LLM。
 * 集成 AiServices 自动工具调用与 ChatMemory。
 *
 * @author lij
 * @since 1.0
 */
public interface ToolOrchestrator {
    
    /**
     * 根据路由决策编排执行
     *
     * @param request  Agent 请求上下文
     * @param decision 路由决策
     * @return Agent 响应（含 LLM 输出和工具调用结果）
     */
    AgentResponse orchestrate(AgentRequest request, ToolRouteDecision decision);
}
