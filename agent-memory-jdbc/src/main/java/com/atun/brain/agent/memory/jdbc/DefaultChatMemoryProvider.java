package com.atun.brain.agent.memory.jdbc;

import com.atun.brain.agent.memory.spi.ChatMemoryProvider;
import com.atun.brain.agent.memory.spi.ChatMemoryStore;
import com.atun.brain.agent.memory.spi.MemoryWindowConfig;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 ChatMemoryProvider 实现
 * <p>
 * 基于 MessageWindowChatMemory + JdbcChatMemoryStore 提供有状态的对话记忆。
 * 规范：ChatMemory 必须通过 ChatMemoryProvider 获取，禁止 new 实例化。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class DefaultChatMemoryProvider implements ChatMemoryProvider {
    
    private final ChatMemoryStore chatMemoryStore;
    private final MemoryWindowConfig windowConfig;
    private final ConcurrentHashMap<String, ChatMemory> memoryCache = new ConcurrentHashMap<>();
    
    public DefaultChatMemoryProvider(ChatMemoryStore chatMemoryStore,
                                      MemoryWindowConfig windowConfig) {
        this.chatMemoryStore = chatMemoryStore;
        this.windowConfig = windowConfig;
        log.info("初始化 ChatMemoryProvider: strategy={}, maxMessages={}, maxTokens={}",
                windowConfig.strategy(), windowConfig.maxMessages(), windowConfig.maxTokens());
    }
    
    @Override
    public ChatMemory getMemory(Object memoryId) {
        String id = memoryId.toString();
        
        return memoryCache.computeIfAbsent(id, key -> {
            log.debug("创建新的 ChatMemory: memoryId={}", key);
            
            // 使用适配器将 SPI ChatMemoryStore 桥接到 LangChain4j 的 ChatMemoryStore
            var lc4jStore = new Lc4jChatMemoryStoreAdapter(chatMemoryStore);
            
            return MessageWindowChatMemory.builder()
                    .id(key)
                    .maxMessages(windowConfig.maxMessages())
                    .chatMemoryStore(lc4jStore)
                    .build();
        });
    }
    
    @Override
    public void evict(Object memoryId) {
        String id = memoryId.toString();
        memoryCache.remove(id);
        chatMemoryStore.deleteMessages(id);
        log.debug("清除记忆: memoryId={}", id);
    }
    
    @Override
    public void evictAll() {
        memoryCache.clear();
        log.info("清除所有记忆缓存");
    }
    
    /**
     * SPI ChatMemoryStore → LangChain4j ChatMemoryStore 适配器
     */
    private static class Lc4jChatMemoryStoreAdapter implements dev.langchain4j.store.memory.chat.ChatMemoryStore {
        
        private final ChatMemoryStore delegate;
        
        Lc4jChatMemoryStoreAdapter(ChatMemoryStore delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public java.util.List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
            return delegate.getMessages(memoryId);
        }
        
        @Override
        public void updateMessages(Object memoryId, java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
            delegate.updateMessages(memoryId, messages);
        }
        
        @Override
        public void deleteMessages(Object memoryId) {
            delegate.deleteMessages(memoryId);
        }
    }
}
