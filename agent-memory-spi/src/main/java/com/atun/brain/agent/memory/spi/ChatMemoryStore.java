package com.atun.brain.agent.memory.spi;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * ChatMemory 持久化存储 SPI
 * <p>
 * 为短期对话记忆提供可插拔的后端存储（JDBC / Redis / MongoDB）。
 * 实现类需要保证线程安全。
 *
 * @author lij
 * @since 1.0
 */
public interface ChatMemoryStore {
    
    /**
     * 获取指定会话的消息列表
     *
     * @param memoryId 记忆标识
     * @return 消息列表（按时间正序排列），无记录时返回空列表
     */
    List<ChatMessage> getMessages(Object memoryId);
    
    /**
     * 更新指定会话的消息列表（覆盖写）
     *
     * @param memoryId 记忆标识
     * @param messages 完整的消息列表
     */
    void updateMessages(Object memoryId, List<ChatMessage> messages);
    
    /**
     * 删除指定会话的所有消息
     *
     * @param memoryId 记忆标识
     */
    void deleteMessages(Object memoryId);
}
