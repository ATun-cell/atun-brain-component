package com.atun.brain.agent.core.exception;

/**
 * Agent 基础异常 - 所有 Agent 相关异常的父类
 * <p>
 * 异常分类规范：
 * <ul>
 *   <li><strong>阻断性异常（需捕获处理）</strong>：
 *     <ul>
 *       <li>AgentConfigurationException - 配置错误</li>
 *       <li>AgentValidationException - 参数校验失败</li>
 *       <li>ToolExecutionException - 工具执行失败</li>
 *     </ul>
 *   </li>
 *   <li><strong>非阻断性异常（记录日志即可）</strong>：
 *     <ul>
 *       <li>AgentNonCriticalException - 不影响主流程的异常</li>
 *     </ul>
 *   </li>
 *   <li><strong>系统异常（向上抛出）</strong>：
 *     <ul>
 *       <li>未捕获的 RuntimeException 及其子类</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author lij
 * @since 1.0
 */
public class AgentException extends RuntimeException {

    /** 错误码 */
    private final String errorCode;

    /** 是否为阻断性异常 */
    private final boolean critical;

    public AgentException(String message) {
        this(message, null, true);
    }

    public AgentException(String message, Throwable cause) {
        this(message, cause, true);
    }

    public AgentException(String message, boolean critical) {
        this(message, null, critical);
    }

    public AgentException(String message, Throwable cause, boolean critical) {
        this("AGENT_ERROR", message, cause, critical);
    }

    public AgentException(String errorCode, String message, Throwable cause, boolean critical) {
        super(message, cause);
        this.errorCode = errorCode;
        this.critical = critical;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isCritical() {
        return critical;
    }
}
