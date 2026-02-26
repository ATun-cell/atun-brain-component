package com.atun.brain.agent.memory.spi;

/**
 * 记忆窗口配置 - 控制短期记忆的容量
 * <p>
 * 规范：短期记忆窗口大小（maxMessages / maxTokens）必须由配置驱动，
 * 默认值不得高于 20（消息数）或 4096（tokens），防止 OOM。
 *
 * @author lij
 * @since 1.0
 */
public record MemoryWindowConfig(
        /** 记忆窗口策略 */
        WindowStrategy strategy,
        /** 最大消息数（MESSAGE 策略生效） */
        int maxMessages,
        /** 最大 token 数（TOKEN 策略生效） */
        int maxTokens
) {
    
    /**
     * 窗口策略枚举
     */
    public enum WindowStrategy {
        /** 基于消息数量的窗口（MessageWindowChatMemory） */
        MESSAGE,
        /** 基于 Token 数量的窗口（TokenWindowChatMemory） */
        TOKEN
    }
    
    /**
     * 默认配置：消息窗口，最大 20 条
     */
    public static MemoryWindowConfig defaultConfig() {
        return new MemoryWindowConfig(WindowStrategy.MESSAGE, 20, 4096);
    }
    
    /**
     * 消息窗口模式
     */
    public static MemoryWindowConfig messageWindow(int maxMessages) {
        if (maxMessages <= 0 || maxMessages > 100) {
            throw new IllegalArgumentException("maxMessages 必须在 1-100 之间，当前值: " + maxMessages);
        }
        return new MemoryWindowConfig(WindowStrategy.MESSAGE, maxMessages, 4096);
    }
    
    /**
     * Token 窗口模式
     */
    public static MemoryWindowConfig tokenWindow(int maxTokens) {
        if (maxTokens <= 0 || maxTokens > 128000) {
            throw new IllegalArgumentException("maxTokens 必须在 1-128000 之间，当前值: " + maxTokens);
        }
        return new MemoryWindowConfig(WindowStrategy.TOKEN, 20, maxTokens);
    }
}
