package com.atun.brain.agent.core;

import com.atun.brain.agent.core.exception.AgentException;
import com.atun.brain.agent.core.exception.AgentNonCriticalException;
import com.atun.brain.agent.core.exception.ExceptionUtils;
import com.atun.brain.agent.core.model.AgentLogEntry;
import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.model.AgentResponse;
import com.atun.brain.agent.core.pipeline.IntentClassifier;
import com.atun.brain.agent.core.pipeline.MemoryPersister;
import com.atun.brain.agent.core.pipeline.ResponseComposer;
import com.atun.brain.agent.core.pipeline.ToolOrchestrator;
import com.atun.brain.agent.tools.spi.ToolRouteDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent 编排器 - 四阶段 Pipeline 主入口
 * <p>
 * Pipeline 阶段：
 * <ol>
 *   <li><strong>IntentClassifier</strong> - 意图分类，输出 ToolRouteDecision</li>
 *   <li><strong>ToolOrchestrator</strong> - 根据决策执行工具链或直连 LLM</li>
 *   <li><strong>ResponseComposer</strong> - 融合工具结果，应用 RAG 增强</li>
 *   <li><strong>MemoryPersister</strong> - 同步短期记忆 + 异步向量化长期知识</li>
 * </ol>
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class AgentOrchestrator {

    private final IntentClassifier intentClassifier;
    private final ToolOrchestrator toolOrchestrator;
    private final ResponseComposer responseComposer;
    private final MemoryPersister memoryPersister;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(IntentClassifier intentClassifier,
                              ToolOrchestrator toolOrchestrator,
                              ResponseComposer responseComposer,
                              MemoryPersister memoryPersister) {
        this.intentClassifier = intentClassifier;
        this.toolOrchestrator = toolOrchestrator;
        this.responseComposer = responseComposer;
        this.memoryPersister = memoryPersister;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 处理 Agent 请求 - 执行完整的四阶段 Pipeline
     *
     * @param request Agent 请求上下文
     * @return Agent 响应
     */
    public AgentResponse process(AgentRequest request) {
        long pipelineStart = System.currentTimeMillis();

        log.info("[AgentPipeline] 开始处理请求: requestId={}, traceId={}, userId={}, message={}",
                request.getRequestId(), request.getTraceId(),
                request.getUserId(), truncate(request.getUserMessage(), 100));

        try {
            // ========== 参数校验 ==========
            validateRequest(request);

            // ========= Stage 1: Intent Classification =========
            long stageStart = System.currentTimeMillis();
            ToolRouteDecision decision;
            try {
                decision = intentClassifier.classify(request);
                logStage(request, "IntentClassifier", System.currentTimeMillis() - stageStart, decision.toString());
            } catch (Exception e) {
                log.error("[AgentPipeline] Stage=IntentClassifier requestId={} 执行失败",
                        request.getRequestId(), e);
                return handleStageError(request, "intent_classifier", e, pipelineStart);
            }

            // ========= Stage 2: Tool Orchestration =========
            stageStart = System.currentTimeMillis();
            AgentResponse orchestratorResponse;
            try {
                orchestratorResponse = toolOrchestrator.orchestrate(request, decision);
                logStage(request, "ToolOrchestrator", System.currentTimeMillis() - stageStart, null);
            } catch (Exception e) {
                log.error("[AgentPipeline] Stage=ToolOrchestrator requestId={} 执行失败",
                        request.getRequestId(), e);
                return handleStageError(request, "tool_orchestrator", e, pipelineStart);
            }

            // ========= Stage 3: Response Composition (RAG) =========
            stageStart = System.currentTimeMillis();
            AgentResponse finalResponse;
            try {
                finalResponse = responseComposer.compose(request, orchestratorResponse);
                logStage(request, "ResponseComposer", System.currentTimeMillis() - stageStart,
                        "ragApplied=" + finalResponse.ragApplied());
            } catch (Exception e) {
                // RAG 异常不阻断主流程，记录日志并使用原始响应
                log.warn("[AgentPipeline] Stage=ResponseComposer requestId={} RAG增强失败，降级为直通模式",
                        request.getRequestId(), e);
                finalResponse = orchestratorResponse;
            }

            // ========= Stage 4: Memory Persistence =========
            stageStart = System.currentTimeMillis();
            try {
                memoryPersister.persist(request, finalResponse);
                logStage(request, "MemoryPersister", System.currentTimeMillis() - stageStart, null);
            } catch (Exception e) {
                // 记忆持久化异常不阻断主流程
                log.warn("[AgentPipeline] Stage=MemoryPersister requestId={} 持久化失败（已降级）",
                        request.getRequestId(), e);
                handleNonCriticalError(request, "memory_persister", e);
            }

            // ========= Pipeline 完成 =========
            long totalLatency = System.currentTimeMillis() - pipelineStart;
            log.info("[AgentPipeline] 请求处理完成: requestId={}, totalLatencyMs={}, " +
                            "hasToolCalls={}, ragApplied={}",
                    request.getRequestId(), totalLatency,
                    finalResponse.hasToolCalls(), finalResponse.ragApplied());

            // 记录结构化日志
            logStructured(AgentLogEntry.of(request, "pipeline", "COMPLETE", totalLatency));

            return finalResponse;

        } catch (AgentException e) {
            // AgentException 已有完整上下文，直接处理
            long totalLatency = System.currentTimeMillis() - pipelineStart;
            log.error("[AgentPipeline] requestId={} 业务异常: {}",
                    request.getRequestId(), ExceptionUtils.extractRootMessage(e), e);

            logStructured(AgentLogEntry.error(request, "pipeline", "ERROR",
                    totalLatency, e.getErrorCode()));

            return AgentResponse.error(request.getRequestId(), e.getMessage(), totalLatency);

        } catch (Exception e) {
            // 未知异常 - 安全兜底
            long totalLatency = System.currentTimeMillis() - pipelineStart;
            log.error("[AgentPipeline] requestId={} 未知异常", request.getRequestId(), e);

            logStructured(AgentLogEntry.error(request, "pipeline", "SYSTEM_ERROR",
                    totalLatency, "SYSTEM_ERROR"));

            return AgentResponse.error(request.getRequestId(),
                    "系统繁忙，请稍后重试", totalLatency);
        }
    }

    /**
     * 简化入口 - 从 userId + message 快速创建请求并处理
     */
    public String chat(Long userId, String message) {
        AgentRequest request = AgentRequest.builder()
                .userId(userId)
                .userMessage(message)
                .build();

        AgentResponse response = process(request);
        return response.message();
    }

    /**
     * 带会话ID的入口
     */
    public String chat(Long userId, String sessionId, String message) {
        AgentRequest request = AgentRequest.builder()
                .userId(userId)
                .sessionId(sessionId)
                .userMessage(message)
                .build();

        AgentResponse response = process(request);
        return response.message();
    }

    // ========== 内部方法 ==========

    /**
     * 请求参数校验
     */
    private void validateRequest(AgentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求对象不能为空");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (request.getUserMessage() == null || request.getUserMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("用户消息不能为空");
        }
    }

    /**
     * 处理阶段错误（阻断性异常）
     */
    private AgentResponse handleStageError(AgentRequest request, String stage,
                                            Throwable e, long pipelineStart) {
        long latencyMs = System.currentTimeMillis() - pipelineStart;
        String errorCode = e instanceof AgentException ae ? ae.getErrorCode() : "UNKNOWN_ERROR";

        logStructured(AgentLogEntry.error(request, "pipeline", stage, latencyMs, errorCode));

        String errorMsg = e instanceof AgentException
                ? e.getMessage()
                : String.format("处理失败，请稍后重试（%s）", ExceptionUtils.getShortIdentifier(e));

        return AgentResponse.error(request.getRequestId(), errorMsg, latencyMs);
    }

    /**
     * 处理非关键异常（非阻断性）
     */
    private void handleNonCriticalError(AgentRequest request, String stage, Throwable e) {
        try {
            AgentNonCriticalException nonCriticalEx = ExceptionUtils.asNonCritical(
                    String.format("非关键阶段 [%s] 执行失败", stage), e);
            log.debug("[AgentPipeline] 非关键异常已记录: {}", nonCriticalEx.getMessage());
        } catch (Exception ex) {
            // 双重保护：记录非关键异常失败时静默处理
            log.trace("记录非关键异常失败", ex);
        }
    }

    private void logStage(AgentRequest request, String stage, long latencyMs, String detail) {
        log.debug("[AgentPipeline] Stage={}, requestId={}, latencyMs={}, detail={}",
                stage, request.getRequestId(), latencyMs, detail);
    }

    private void logStructured(AgentLogEntry entry) {
        try {
            log.info("[AgentLog] {}", objectMapper.writeValueAsString(entry));
        } catch (Exception e) {
            log.warn("结构化日志序列化失败", e);
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
