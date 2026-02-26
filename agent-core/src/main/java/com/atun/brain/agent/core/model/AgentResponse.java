package com.atun.brain.agent.core.model;

import java.time.Instant;
import java.util.List;

/**
 * Agent 响应 - Pipeline 最终输出
 *
 * @author lij
 * @since 1.0
 */
public record AgentResponse(
        /** 请求ID（关联原始请求） */
        String requestId,
        /** AI 回复文本 */
        String message,
        /** 是否调用了工具 */
        boolean hasToolCalls,
        /** 使用的工具列表 */
        List<String> toolsUsed,
        /** 是否应用了 RAG */
        boolean ragApplied,
        /** 输入 token 数 */
        int inputTokens,
        /** 输出 token 数 */
        int outputTokens,
        /** 处理耗时（毫秒） */
        long latencyMs,
        /** 响应时间戳 */
        Instant timestamp
) {
    
    /**
     * 创建简单响应
     */
    public static AgentResponse simple(String requestId, String message, long latencyMs) {
        return new AgentResponse(
                requestId, message, false, List.of(),
                false, 0, 0, latencyMs, Instant.now()
        );
    }
    
    /**
     * 创建带工具调用的响应
     */
    public static AgentResponse withTools(String requestId, String message,
                                           List<String> toolsUsed, long latencyMs) {
        return new AgentResponse(
                requestId, message, true, toolsUsed,
                false, 0, 0, latencyMs, Instant.now()
        );
    }
    
    /**
     * 创建错误响应
     */
    public static AgentResponse error(String requestId, String errorMessage, long latencyMs) {
        return new AgentResponse(
                requestId, "抱歉，处理您的请求时出现错误：" + errorMessage,
                false, List.of(), false, 0, 0, latencyMs, Instant.now()
        );
    }
}
