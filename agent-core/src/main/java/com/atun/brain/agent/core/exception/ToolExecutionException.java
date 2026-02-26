package com.atun.brain.agent.core.exception;

/**
 * 工具执行异常 - 工具调用失败导致的阻断性异常
 * <p>
 * 使用场景：
 * <ul>
 *   <li>工具方法执行抛出异常</li>
 *   <li>工具依赖的外部服务调用失败</li>
 *   <li>工具返回结果不符合预期格式</li>
 * </ul>
 *
 * @author lij
 * @since 1.0
 */
public class ToolExecutionException extends AgentException {

    /** 工具名称 */
    private final String toolName;

    public ToolExecutionException(String toolName, String message) {
        super("TOOL_EXECUTION_ERROR", formatMessage(toolName, message), null, true);
        this.toolName = toolName;
    }

    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super("TOOL_EXECUTION_ERROR", formatMessage(toolName, message), cause, true);
        this.toolName = toolName;
    }

    private static String formatMessage(String toolName, String message) {
        return String.format("工具 [%s] 执行失败: %s", toolName, message);
    }

    public String getToolName() {
        return toolName;
    }
}
