package com.atun.brain.agent.core.exception;

/**
 * Agent 验证异常 - 参数校验失败导致的阻断性异常
 * <p>
 * 使用场景：
 * <ul>
 *   <li>请求参数缺失或格式错误</li>
 *   <li>用户输入不符合业务规则</li>
 *   <li>必要字段为空</li>
 * </ul>
 *
 * @author lij
 * @since 1.0
 */
public class AgentValidationException extends AgentException {

    public AgentValidationException(String message) {
        super("AGENT_VALIDATION_ERROR", message, null, true);
    }

    public AgentValidationException(String fieldName, String reason) {
        super("AGENT_VALIDATION_ERROR", String.format("字段 [%s] 验证失败: %s", fieldName, reason), null, true);
    }
}
