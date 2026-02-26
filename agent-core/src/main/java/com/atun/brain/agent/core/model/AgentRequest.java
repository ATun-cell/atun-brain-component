package com.atun.brain.agent.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 请求上下文 - 贯穿四阶段 Pipeline 的上下文对象
 * <p>
 * 规范：所有 LLM 请求必须携带 requestId 与 traceId，并记录至日志（JSON 格式）。
 *
 * @author lij
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
public class AgentRequest {
    
    /** 请求ID（全局唯一） */
    private final String requestId;
    
    /** 链路追踪ID */
    private final String traceId;
    
    /** 用户ID */
    private final Long userId;
    
    /** 会话标识（memoryId） */
    private final String sessionId;
    
    /** 用户消息 */
    private final String userMessage;
    
    /** 请求时间戳 */
    private final Instant timestamp;
    
    /** 扩展属性 */
    private final Map<String, Object> attributes;
    /**
     * 获取 memoryId（用于 ChatMemoryProvider 路由）
     * 格式：user:{userId}:session:{sessionId}
     */
    public String getMemoryId() {
        if (sessionId != null) {
            return "user:" + userId + ":session:" + sessionId;
        }
        return "user:" + userId;
    }
}
