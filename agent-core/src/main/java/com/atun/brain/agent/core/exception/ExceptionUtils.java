package com.atun.brain.agent.core.exception;

/**
 * Agent 异常处理工具类
 * <p>
 * 提供统一的异常处理辅助方法。
 *
 * @author lij
 * @since 1.0
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
        // 静态工具类，禁止实例化
    }

    /**
     * 判断是否为阻断性异常
     *
     * @param e 异常
     * @return true - 阻断性; false - 非阻断性
     */
    public static boolean isCritical(Throwable e) {
        if (e instanceof AgentException agentEx) {
            return agentEx.isCritical();
        }
        // 默认所有 RuntimeException 都是阻断性的
        return e instanceof RuntimeException;
    }

    /**
     * 包装为非关键异常
     *
     * @param message 错误消息
     * @param cause 原始异常
     * @return AgentNonCriticalException
     */
    public static AgentNonCriticalException asNonCritical(String message, Throwable cause) {
        return new AgentNonCriticalException(message, cause);
    }

    /**
     * 提取根异常消息（最多3层）
     *
     * @param e 异常
     * @return 格式化的错误消息
     */
    public static String extractRootMessage(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable current = e;
        int depth = 0;

        while (current != null && depth < 3) {
            if (depth > 0) {
                sb.append(" -> ");
            }
            sb.append(current.getClass().getSimpleName());
            if (current.getMessage() != null) {
                sb.append(": ").append(current.getMessage());
            }
            current = current.getCause();
            depth++;
        }

        return sb.toString();
    }

    /**
     * 获取简短的异常标识（用于日志标记）
     *
     * @param e 异常
     * @return 简短标识
     */
    public static String getShortIdentifier(Throwable e) {
        if (e instanceof AgentException agentEx) {
            return agentEx.getErrorCode();
        }
        return e.getClass().getSimpleName();
    }
}
