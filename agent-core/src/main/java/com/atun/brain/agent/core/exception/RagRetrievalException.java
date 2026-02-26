package com.atun.brain.agent.core.exception;

/**
 * RAG 检索异常 - 向量检索失败导致的阻断性异常
 * <p>
 * 使用场景：
 * <ul>
 *   <li>向量数据库连接失败</li>
 *   <li>向量搜索超时</li>
 *   <li>嵌入模型调用失败</li>
 * </ul>
 *
 * @author lij
 * @since 1.0
 */
public class RagRetrievalException extends AgentException {

    public RagRetrievalException(String message) {
        super("RAG_RETRIEVAL_ERROR", message, null, true);
    }

    public RagRetrievalException(String message, Throwable cause) {
        super("RAG_RETRIEVAL_ERROR", message, cause, true);
    }
}
