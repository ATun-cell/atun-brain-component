package com.atun.brain.agent.memory.jdbc;

import com.atun.brain.agent.memory.spi.ChatMemoryStore;
import com.atun.brain.agent.core.exception.MemoryStorageException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 JDBC 的 ChatMemoryStore 实现
 * <p>
 * 将对话消息以 JSON 序列化方式存储到 MySQL 的 chat_memory 表中。
 * 线程安全（依赖 JdbcTemplate 的线程安全性）。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class JdbcChatMemoryStore implements ChatMemoryStore {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String TABLE_NAME = "chat_memory";
    
    public JdbcChatMemoryStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        ensureTableExists();
    }
    
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = memoryId.toString();

        try {
            List<String> jsonList = jdbcTemplate.query(
                    "SELECT messages_json FROM " + TABLE_NAME + " WHERE memory_id = ?",
                    (rs, rowNum) -> rs.getString("messages_json"),
                    id
            );

            if (jsonList.isEmpty()) {
                return new ArrayList<>();
            }

            return deserializeMessages(jsonList.get(0));

        } catch (DataAccessException e) {
            log.error("数据库查询失败: memoryId={}", id, e);
            throw new MemoryStorageException("数据库查询失败: " + id, e);
        } catch (JsonProcessingException e) {
            log.error("反序列化消息失败: memoryId={}", id, e);
            throw new MemoryStorageException("消息格式错误: " + id, e);
        } catch (Exception e) {
            log.error("获取记忆失败: memoryId={}", id, e);
            throw new MemoryStorageException("获取记忆失败: " + id, e);
        }
    }
    
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = memoryId.toString();

        try {
            String json = serializeMessages(messages);

            int updated = jdbcTemplate.update(
                    "UPDATE " + TABLE_NAME + " SET messages_json = ?, updated_at = NOW() WHERE memory_id = ?",
                    json, id
            );

            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO " + TABLE_NAME + " (memory_id, messages_json, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                        id, json
                );
            }

            log.debug("更新记忆成功: memoryId={}, messageCount={}", id, messages.size());

        } catch (DataAccessException e) {
            log.error("数据库更新失败: memoryId={}", id, e);
            throw new MemoryStorageException("数据库更新失败: " + id, e);
        } catch (JsonProcessingException e) {
            log.error("序列化消息失败: memoryId={}", id, e);
            throw new MemoryStorageException("消息序列化失败: " + id, e);
        } catch (Exception e) {
            log.error("更新记忆失败: memoryId={}", id, e);
            throw new MemoryStorageException("更新记忆失败: " + id, e);
        }
    }
    
    @Override
    public void deleteMessages(Object memoryId) {
        String id = memoryId.toString();
        try {
            jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE memory_id = ?", id);
            log.debug("删除记忆: memoryId={}", id);
        } catch (DataAccessException e) {
            log.error("数据库删除失败: memoryId={}", id, e);
            throw new MemoryStorageException("数据库删除失败: " + id, e);
        } catch (Exception e) {
            log.error("删除记忆失败: memoryId={}", id, e);
            throw new MemoryStorageException("删除记忆失败: " + id, e);
        }
    }
    
    // ========== 序列化 ==========
    
    private String serializeMessages(List<ChatMessage> messages) throws JsonProcessingException {
        List<Map<String, String>> serialized = messages.stream()
                .map(msg -> {
                    String role;
                    String content;
                    if (msg instanceof SystemMessage sm) {
                        role = "system";
                        content = sm.text();
                    } else if (msg instanceof UserMessage um) {
                        role = "user";
                        content = um.singleText();
                    } else if (msg instanceof AiMessage am) {
                        role = "assistant";
                        content = am.text();
                    } else {
                        role = "unknown";
                        content = msg.toString();
                    }
                    return Map.of("role", role, "content", content);
                })
                .toList();
        
        return objectMapper.writeValueAsString(serialized);
    }
    
    private List<ChatMessage> deserializeMessages(String json) throws JsonProcessingException {
        List<Map<String, String>> serialized = objectMapper.readValue(
                json, new TypeReference<>() {}
        );
        
        List<ChatMessage> messages = new ArrayList<>();
        for (Map<String, String> entry : serialized) {
            String role = entry.get("role");
            String content = entry.get("content");
            
            switch (role) {
                case "system" -> messages.add(SystemMessage.from(content));
                case "user" -> messages.add(UserMessage.from(content));
                case "assistant" -> messages.add(AiMessage.from(content));
                default -> log.warn("未知的消息角色: {}", role);
            }
        }
        
        return messages;
    }
    
    // ========== DDL ==========
    
    private void ensureTableExists() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_memory (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    memory_id VARCHAR(255) NOT NULL UNIQUE,
                    messages_json LONGTEXT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_memory_id (memory_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            log.info("chat_memory 表已就绪");
        } catch (DataAccessException e) {
            log.error("创建 chat_memory 表失败", e);
            throw new MemoryStorageException("初始化数据库表失败", e);
        } catch (Exception e) {
            log.warn("创建 chat_memory 表失败（可能已存在）: {}", e.getMessage());
        }
    }
}
