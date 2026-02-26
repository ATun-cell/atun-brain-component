package com.atun.brain.agent.config;

import com.atun.brain.agent.memory.jdbc.DefaultChatMemoryProvider;
import com.atun.brain.agent.memory.jdbc.JdbcChatMemoryStore;
import com.atun.brain.agent.memory.jdbc.MemoryPersistEventListener;
import com.atun.brain.agent.memory.spi.ChatMemoryProvider;
import com.atun.brain.agent.memory.spi.ChatMemoryStore;
import com.atun.brain.agent.memory.spi.MemoryWindowConfig;
import com.atun.brain.agent.rag.spi.RetrievalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 记忆层自动配置
 * <p>
 * 条件化注册 ChatMemoryStore / ChatMemoryProvider Bean。
 * 默认使用 JDBC 存储，可通过配置切换。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "agent.memory", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentMemoryAutoConfiguration {
    
    private final AgentProperties agentProperties;
    
    /**
     * 记忆窗口配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MemoryWindowConfig memoryWindowConfig() {
        AgentProperties.MemoryConfig memoryConfig = agentProperties.getMemory();
        
        return switch (memoryConfig.getStrategy().toLowerCase()) {
            case "token" -> MemoryWindowConfig.tokenWindow(memoryConfig.getMaxTokens());
            default -> MemoryWindowConfig.messageWindow(memoryConfig.getMaxMessages());
        };
    }
    
    /**
     * JDBC ChatMemoryStore（条件：classpath 包含 JdbcTemplate）
     */
    @Bean
    @ConditionalOnMissingBean(ChatMemoryStore.class)
    @ConditionalOnClass(JdbcTemplate.class)
    @ConditionalOnProperty(prefix = "agent.memory", name = "store", havingValue = "jdbc", matchIfMissing = true)
    public ChatMemoryStore jdbcChatMemoryStore(JdbcTemplate jdbcTemplate) {
        log.info("初始化 JDBC ChatMemoryStore");
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        return new JdbcChatMemoryStore(jdbcTemplate, om);
    }
    
    /**
     * ChatMemoryProvider
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore,
                                                  MemoryWindowConfig windowConfig) {
        return new DefaultChatMemoryProvider(chatMemoryStore, windowConfig);
    }
    
    /**
     * 记忆持久化事件监听器（双写：短期记忆 → 长期知识库）
     */
    @Bean
    @ConditionalOnClass(JdbcTemplate.class)
    public MemoryPersistEventListener memoryPersistEventListener(
            JdbcTemplate jdbcTemplate,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            RetrievalService retrievalService) {
        
        MemoryPersistEventListener.MemoryToRagCallback callback = null;
        
        if (retrievalService != null) {
            callback = (userId, userMessage, assistantMessage, summary) -> {
                // 将对话内容向量化入库
                String text = summary != null ? summary
                        : "用户: " + userMessage + "\n助手: " + assistantMessage;
                
                retrievalService.ingest(text, java.util.Map.of(
                        "userId", userId.toString(),
                        "type", "conversation"
                ));
            };
            log.info("记忆持久化事件监听器已配置 RAG 回调");
        }
        
        return new MemoryPersistEventListener(jdbcTemplate, callback);
    }
}
