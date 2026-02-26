package com.atun.brain.agent.core.exception;

/**
 * Agent 非关键异常 - 不影响主流程的异常（非阻断性）
 * <p>
 * 使用场景：
 * <ul>
 *   <li>死信队列写入失败</li>
 *   <li>监控指标上报失败</li>
 *   <li>日志记录失败</li>
 *   <li>异步任务执行失败</li>
 * </ul>
 * <p>
 * 特性：
 * <ul>
 *   <li>不会中断主业务流程</li>
 *   <li>仅记录日志，不向上抛出</li>
 *   <li>适合在 finally 或 catch 块中处理</li>
 * </ul>
 *
 * @author lij
 * @since 1.0
 */
public class AgentNonCriticalException extends AgentException {

    public AgentNonCriticalException(String message) {
        super("AGENT_NON_CRITICAL", message, null, false);
    }

    public AgentNonCriticalException(String message, Throwable cause) {
        super("AGENT_NON_CRITICAL", message, cause, false);
    }
}
