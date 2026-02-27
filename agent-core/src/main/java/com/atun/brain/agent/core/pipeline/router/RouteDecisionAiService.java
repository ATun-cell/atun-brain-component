package com.atun.brain.agent.core.pipeline.router;

import com.atun.brain.agent.tools.spi.ToolRouteDecision;

/**
 * 智能路由决策 AI 服务
 * <p>
 * 通过 LLM 理解用户意图，结合已注册的工具列表和流程编排器列表，
 * 智能判断用户输入应该走哪种路由策略：
 * - DIRECT_LLM：简单闲聊、问候、无需工具的场景
 * - DIRECT_TOOL：需要调用特定工具的场景
 * - ORCHESTRATED_FLOW：需要走流程编排器的复杂业务场景（多工具调用也走流程）
 *
 * @author lij
 * @since 1.0
 */
public interface RouteDecisionAiService {

    /**
     * 根据用户输入和可用工具/流程列表，智能判断路由策略
     *
     * @param userMessage 用户输入消息
     * @param context 路由上下文（包含工具和流程信息）
     * @return 路由决策
     */
    ToolRouteDecision decide(String userMessage, RouteContext context);

    /**
     * 路由上下文
     *
     * @param availableTools 可用工具列表（工具名：工具描述）
     * @param availableFlows 可用流程编排器列表（流程名：流程描述）
     */
    record RouteContext(
            String[] availableTools,
            String[] availableFlows
    ) {
    }
}