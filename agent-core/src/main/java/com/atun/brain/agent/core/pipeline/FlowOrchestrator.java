package com.atun.brain.agent.core.pipeline;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.model.AgentResponse;

/**
 * 流程编排器 SPI
 * <p>
 * 用于处理复杂业务场景的多步骤编排流程。
 * 实现类通过 @Flow 注解标识，由 FlowRegistry 统一注册和管理。
 *
 * @author lij
 * @since 1.0
 */
public interface FlowOrchestrator {

    /**
     * 获取流程标识符（与 @Flow 注解的 name 保持一致）
     *
     * @return 流程唯一标识，如 "financial_analysis"、"multi_step_research"
     */
    String getFlowName();

    /**
     * 获取流程描述（用于 IntentClassifier 决策参考）
     *
     * @return 流程功能描述
     */
    String getDescription();

    /**
     * 执行编排流程
     *
     * @param context 编排上下文（包含原始请求和流程状态）
     * @return Agent 响应
     */
    AgentResponse execute(FlowContext context);

    /**
     * 是否支持当前请求的编排
     * <p>
     * 用于在多个 FlowOrchestrator 中选择合适的实现
     *
     * @param context 编排上下文
     * @return true 表示支持此流程
     */
    default boolean supports(FlowContext context) {
        return true;
    }
}
