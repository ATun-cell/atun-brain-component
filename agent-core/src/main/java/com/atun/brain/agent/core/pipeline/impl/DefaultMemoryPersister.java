package com.atun.brain.agent.core.pipeline.impl;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.model.AgentResponse;
import com.atun.brain.agent.core.pipeline.MemoryPersister;
import com.atun.brain.agent.memory.spi.MemoryPersistEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 默认记忆持久化器实现
 * <p>
 * 工作流程：
 * 1. 短期记忆由 AiServices 的 ChatMemory 自动管理（已在 ToolOrchestrator 中配置）
 * 2. 通过 Spring ApplicationEvent 异步通知 RAG 模块将对话摘要向量化入库
 * 3. 监听器端失败时进入死信队列（由 agent-memory-jdbc 实现）
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class DefaultMemoryPersister implements MemoryPersister {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public DefaultMemoryPersister(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public void persist(AgentRequest request, AgentResponse response) {
        try {
            // 短期记忆已由 AiServices ChatMemory 自动管理
            // 这里只负责触发长期知识库的异步向量化
            
            MemoryPersistEvent event = MemoryPersistEvent.of(
                    request.getMemoryId(),
                    request.getUserId(),
                    request.getUserMessage(),
                    response.message()
            );
            
            // 发布 Spring 事件（异步由 @Async 监听器处理）
            eventPublisher.publishEvent(event);
            
            log.debug("[MemoryPersister] requestId={} 已发布记忆持久化事件, memoryId={}",
                    request.getRequestId(), request.getMemoryId());
            
        } catch (Exception e) {
            // 记忆持久化不应阻断主流程
            log.error("[MemoryPersister] requestId={} 发布事件失败",
                    request.getRequestId(), e);
        }
    }
}
