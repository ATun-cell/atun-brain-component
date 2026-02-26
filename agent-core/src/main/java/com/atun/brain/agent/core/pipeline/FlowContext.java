package com.atun.brain.agent.core.pipeline;

import com.atun.brain.agent.core.model.AgentRequest;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 编排流程上下文 - 在 FlowOrchestrator 执行过程中传递状态
 * <p>
 * 设计目标：
 * - 携带原始请求信息
 * - 支持多步骤之间的状态共享
 * - 支持临时变量存储
 *
 * @author lij
 * @since 1.0
 */
@Data
@Builder
public class FlowContext {

    /** 原始 Agent 请求 */
    private final AgentRequest request;

    /** 流程名称（与 FlowOrchestrator.getFlowName() 对应） */
    private final String flowName;

    /** 当前步骤索引（从 0 开始） */
    @Builder.Default
    private int currentStep = 0;

    /** 流程状态（用于多步骤间共享） */
    @Builder.Default
    private final Map<String, Object> state = new HashMap<>();

    /** 扩展属性（业务自定义） */
    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * 获取状态值
     *
     * @param key 状态键
     * @param <T> 目标类型
     * @return 状态值
     */
    @SuppressWarnings("unchecked")
    public <T> T getState(String key) {
        return (T) state.get(key);
    }

    /**
     * 设置状态值
     *
     * @param key 状态键
     * @param value 状态值
     */
    public void setState(String key, Object value) {
        state.put(key, value);
    }

    /**
     * 获取扩展属性
     *
     * @param key 属性键
     * @param <T> 目标类型
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 设置扩展属性
     *
     * @param key 属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 推进到下一步
     */
    public void nextStep() {
        this.currentStep++;
    }
}
