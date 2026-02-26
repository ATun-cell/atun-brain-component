package com.atun.brain.agent.core.pipeline;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.tools.spi.ToolRouteDecision;

/**
 * Pipeline 第一阶段：意图分类器
 * <p>
 * 轻量级分类器，基于首轮消息判断工具路由策略，
 * 减少不必要的工具调用开销。
 *
 * @author lij
 * @since 1.0
 */
public interface IntentClassifier {
    
    /**
     * 分类用户意图，输出工具路由决策
     *
     * @param request Agent 请求上下文
     * @return 路由决策（DIRECT_LLM / DIRECT_TOOL / AUTO_TOOL_CHAIN）
     */
    ToolRouteDecision classify(AgentRequest request);
}
