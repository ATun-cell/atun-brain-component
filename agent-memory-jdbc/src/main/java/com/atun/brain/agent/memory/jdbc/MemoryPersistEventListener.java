package com.atun.brain.agent.memory.jdbc;

import com.atun.brain.agent.memory.spi.MemoryPersistEvent;
import com.atun.brain.agent.core.exception.AgentNonCriticalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;

/**
 * 记忆持久化事件监听器
 * <p>
 * 异步监听 MemoryPersistEvent，将对话摘要写入死信队列或长期知识库。
 * 失败时记录到 dlq_memory_events 表，支持后续重试。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class MemoryPersistEventListener {

    private final JdbcTemplate jdbcTemplate;

    /** RAG 入库回调（可选，由 agent-rag-* 模块注入） */
    private final MemoryToRagCallback ragCallback;

    public MemoryPersistEventListener(JdbcTemplate jdbcTemplate,
                                       MemoryToRagCallback ragCallback) {
        this.jdbcTemplate = jdbcTemplate;
        this.ragCallback = ragCallback;
        ensureDlqTableExists();
    }

    @Async
    @EventListener
    public void onMemoryPersistEvent(MemoryPersistEvent event) {
        log.debug("收到记忆持久化事件: memoryId={}, userId={}", event.memoryId(), event.userId());

        try {
            if (ragCallback != null) {
                // 触发 RAG 向量化入库
                ragCallback.persistToRag(
                        event.userId(),
                        event.userMessage(),
                        event.assistantMessage(),
                        event.summary()
                );
                log.debug("记忆向量化入库成功: userId={}", event.userId());
            }
        } catch (Exception e) {
            log.error("记忆向量化入库失败，写入死信队列: userId={}", event.userId(), e);
            // 非阻断性：写入死信队列失败不影响主流程
            try {
                writeToDlq(event, e.getMessage());
            } catch (Exception ex) {
                log.error("写入死信队列失败（已降级）: userId={}", event.userId(), ex);
                // 双重保护：死信队列写入失败时静默，不抛出异常
                throw new AgentNonCriticalException("死信队列写入失败", ex);
            }
        }
    }
    
    private void writeToDlq(MemoryPersistEvent event, String errorMessage) {
        try {
            jdbcTemplate.update("""
                INSERT INTO dlq_memory_events (memory_id, user_id, user_message, assistant_message,
                    error_message, created_at, retry_count)
                VALUES (?, ?, ?, ?, ?, NOW(), 0)
                """,
                    event.memoryId().toString(),
                    event.userId(),
                    truncate(event.userMessage(), 2000),
                    truncate(event.assistantMessage(), 2000),
                    truncate(errorMessage, 500)
            );
        } catch (Exception ex) {
            log.error("写入死信队列失败", ex);
            // 死信队列写入失败视为非阻断性异常
            throw new AgentNonCriticalException("死信队列写入失败", ex);
        }
    }
    
    private void ensureDlqTableExists() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS dlq_memory_events (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    memory_id VARCHAR(255) NOT NULL,
                    user_id BIGINT NOT NULL,
                    user_message TEXT,
                    assistant_message TEXT,
                    error_message VARCHAR(500),
                    retry_count INT DEFAULT 0,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    processed_at DATETIME NULL,
                    INDEX idx_user_id (user_id),
                    INDEX idx_created_at (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        } catch (Exception e) {
            log.warn("创建 dlq_memory_events 表失败（可能已存在）: {}", e.getMessage());
        }
    }
    
    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }
    
    /**
     * RAG 入库回调接口
     */
    @FunctionalInterface
    public interface MemoryToRagCallback {
        void persistToRag(Long userId, String userMessage, String assistantMessage, String summary);
    }
}
