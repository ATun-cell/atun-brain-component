package com.atun.brain.agent.core.model;

/**
 * Agent 日志条目 - 满足全链路可观测性要求
 * <p>
 * 规范：所有 LLM 请求必须记录以下字段至 JSON 格式日志。
 *
 * @author lij
 * @since 1.0
 */
public record AgentLogEntry(
        /** 请求ID */
        String requestId,
        /** 链路追踪ID */
        String traceId,
        /** 模型名称 */
        String modelName,
        /** 输入 token 数 */
        int inputTokens,
        /** 输出 token 数 */
        int outputTokens,
        /** 处理延迟（毫秒） */
        long latencyMs,
        /** 是否为流式响应 */
        boolean isStreaming,
        /** 错误代码（无错误时为 null） */
        String errorCode,
        /** Pipeline 阶段 */
        String pipelineStage
) {
    
    /**
     * 构建日志条目
     */
    public static AgentLogEntry of(AgentRequest request, String modelName,
                                    String pipelineStage, long latencyMs) {
        return new AgentLogEntry(
                request.getRequestId(), request.getTraceId(),
                modelName, 0, 0, latencyMs, false, null, pipelineStage
        );
    }
    
    /**
     * 构建错误日志条目
     */
    public static AgentLogEntry error(AgentRequest request, String modelName,
                                       String pipelineStage, long latencyMs,
                                       String errorCode) {
        return new AgentLogEntry(
                request.getRequestId(), request.getTraceId(),
                modelName, 0, 0, latencyMs, false, errorCode, pipelineStage
        );
    }
}
