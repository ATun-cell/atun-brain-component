package com.atun.brain.agent.core.exception;

/**
 * 记忆存储异常 - 记忆持久化失败的阻断性异常
 * <p>
 * 使用场景：
 * <ul>
 *   <li>数据库连接失败</li>
 *   <li>SQL 执行错误</li>
 *   <li>序列化/反序列化失败</li>
 * </ul>
 *
 * @author lij
 * @since 1.0
 */
public class MemoryStorageException extends AgentException {

    public MemoryStorageException(String message) {
        super("MEMORY_STORAGE_ERROR", message, null, true);
    }

    public MemoryStorageException(String message, Throwable cause) {
        super("MEMORY_STORAGE_ERROR", message, cause, true);
    }
}
