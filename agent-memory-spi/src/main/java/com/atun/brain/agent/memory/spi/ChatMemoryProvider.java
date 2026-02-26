package com.atun.brain.agent.memory.spi;

import dev.langchain4j.memory.ChatMemory;

/**
 * ChatMemory 提供者 SPI
 * <p>
 * 规范：ChatMemory 必须通过 ChatMemoryProvider 接口获取，禁止 new 实例化。
 * 支持基于 memoryId 的语义路由（用户身份/设备指纹/会话标签）。
 *
 * @author lij
 * @since 1.0
 */
public interface ChatMemoryProvider {
    
    /**
     * 根据 memoryId 获取或创建 ChatMemory 实例
     * <p>
     * memoryId 通常由 userId + sessionId 组合生成，用于隔离不同会话的记忆。
     *
     * @param memoryId 记忆标识（如 "user:1:session:abc123"）
     * @return ChatMemory 实例
     */
    ChatMemory getMemory(Object memoryId);
    
    /**
     * 清除指定会话的记忆
     *
     * @param memoryId 记忆标识
     */
    void evict(Object memoryId);
    
    /**
     * 清除所有记忆
     */
    void evictAll();
}
