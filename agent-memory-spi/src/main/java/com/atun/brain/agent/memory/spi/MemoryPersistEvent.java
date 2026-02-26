package com.atun.brain.agent.memory.spi;

/**
 * 记忆持久化事件 - 用于短期记忆到长期知识库的双写
 * <p>
 * 当 ChatMemoryStore.updateMessages() 调用后，触发此事件通知
 * RAG 模块将关键对话摘要向量化入库。
 *
 * @author lij
 * @since 1.0
 */
public record MemoryPersistEvent(
        /** 记忆标识 */
        Object memoryId,
        /** 用户ID */
        Long userId,
        /** 用户消息内容 */
        String userMessage,
        /** AI 回复内容 */
        String assistantMessage,
        /** 会话摘要（可选，由摘要器生成） */
        String summary,
        /** 事件时间戳（毫秒） */
        long timestamp
) {
    
    /**
     * 创建持久化事件
     */
    public static MemoryPersistEvent of(Object memoryId, Long userId,
                                         String userMessage, String assistantMessage) {
        return new MemoryPersistEvent(
                memoryId, userId, userMessage, assistantMessage,
                null, System.currentTimeMillis()
        );
    }
    
    /**
     * 带摘要的持久化事件
     */
    public static MemoryPersistEvent withSummary(Object memoryId, Long userId,
                                                  String userMessage, String assistantMessage,
                                                  String summary) {
        return new MemoryPersistEvent(
                memoryId, userId, userMessage, assistantMessage,
                summary, System.currentTimeMillis()
        );
    }
}
