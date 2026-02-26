package com.atun.brain.agent.tools.spi;

/**
 * 工具执行结果 - 所有 @Tool 方法的标准返回类型
 * <p>
 * 规范：@Tool 方法返回类型强制为 String 或 ToolResult，禁止 void 或原始类型，
 * 确保统一序列化与错误传播路径。
 *
 * @author lij
 * @since 1.0
 */
public record ToolResult(
        /** 工具名称 */
        String toolName,
        /** 是否执行成功 */
        boolean success,
        /** 结果数据（序列化为 JSON 或可读文本） */
        String data,
        /** 错误信息（仅 success=false 时有值） */
        String errorMessage,
        /** 执行耗时（毫秒） */
        long latencyMs
) {
    
    /**
     * 创建成功结果
     */
    public static ToolResult success(String toolName, String data, long latencyMs) {
        return new ToolResult(toolName, true, data, null, latencyMs);
    }
    
    /**
     * 创建失败结果
     */
    public static ToolResult failure(String toolName, String errorMessage, long latencyMs) {
        return new ToolResult(toolName, false, null, errorMessage, latencyMs);
    }
    
    /**
     * 转为可读字符串供 LLM 使用
     */
    @Override
    public String toString() {
        if (success) {
            return data;
        }
        return "❌ 工具执行失败 [" + toolName + "]: " + errorMessage;
    }
}
