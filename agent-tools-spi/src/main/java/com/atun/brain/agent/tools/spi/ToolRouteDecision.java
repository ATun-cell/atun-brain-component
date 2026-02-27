package com.atun.brain.agent.tools.spi;

/**
 * 工具路由决策 - IntentClassifier 的输出
 * <p>
 * 描述 Agent 应该采取的工具调用策略。
 *
 * @author lij
 * @since 1.0
 */
public record ToolRouteDecision(
        /** 路由策略 */
        RouteStrategy strategy,
        /** 推荐的工具名称（DIRECT_TOOL 时有值） */
        String suggestedToolName,
        /** 推荐的流程编排器名称（ORCHESTRATED_FLOW 时有值） */
        String flowName,
        /** 分类原因（用于日志追踪） */
        String reason
) {

    /**
     * 路由策略枚举
     */
    public enum RouteStrategy {
        /** 直接调用 LLM 回答（无需工具） */
        DIRECT_LLM,
        /** 调用指定工具后由 LLM 合成回答 */
        DIRECT_TOOL,
        /** 由 FlowOrchestrator 执行自定义编排流程 */
        ORCHESTRATED_FLOW
    }

    /**
     * 直连 LLM
     */
    public static ToolRouteDecision directLlm(String reason) {
        return new ToolRouteDecision(RouteStrategy.DIRECT_LLM, null, null, reason);
    }

    /**
     * 直接调用指定工具
     */
    public static ToolRouteDecision directTool(String toolName, String reason) {
        return new ToolRouteDecision(RouteStrategy.DIRECT_TOOL, toolName, null, reason);
    }

    /**
     * 执行编排流程
     *
     * @param flowName 流程编排器名称
     * @param reason 分类原因
     * @return 路由决策
     */
    public static ToolRouteDecision orchestratedFlow(String flowName, String reason) {
        return new ToolRouteDecision(RouteStrategy.ORCHESTRATED_FLOW, null, flowName, reason);
    }
}
