package com.atun.brain.agent.core.exception;

/**
 * Agent 配置异常 - 配置错误导致的阻断性异常
 * <p>
 * 使用场景：
 * <ul>
 *   <li>配置属性缺失或格式错误</li>
 *   <li>Bean 依赖注入失败</li>
 *   <li>外部服务连接配置错误</li>
 * </ul>
 *
 * @author lij
 * @since 1.0
 */
public class AgentConfigurationException extends AgentException {

    public AgentConfigurationException(String message) {
        super("AGENT_CONFIG_ERROR", message, null, true);
    }

    public AgentConfigurationException(String message, Throwable cause) {
        super("AGENT_CONFIG_ERROR", message, cause, true);
    }
}
