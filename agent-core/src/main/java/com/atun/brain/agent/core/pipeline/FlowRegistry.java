package com.atun.brain.agent.core.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程编排器注册中心
 * <p>
 * 管理所有 FlowOrchestrator 实现，支持按名称查找和按条件匹配。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Component
public class FlowRegistry {

    /** 流程注册表：flowName -> FlowOrchestrator */
    private final Map<String, FlowOrchestrator> flowRegistry = new ConcurrentHashMap<>();

    /**
     * 注册 FlowOrchestrator
     *
     * @param orchestrator 流程编排器
     */
    public void register(FlowOrchestrator orchestrator) {
        String flowName = orchestrator.getFlowName();
        if (flowRegistry.containsKey(flowName)) {
            log.warn("[FlowRegistry] 检测到重复的 flowName: {}，将覆盖已有注册", flowName);
        }
        flowRegistry.put(flowName, orchestrator);
        log.info("[FlowRegistry] 注册 FlowOrchestrator: flowName={}, description={}",
                flowName, orchestrator.getDescription());
    }

    /**
     * 根据 flowName 获取 FlowOrchestrator
     *
     * @param flowName 流程名称
     * @return FlowOrchestrator（可能为空）
     */
    public Optional<FlowOrchestrator> getFlowOrchestrator(String flowName) {
        return Optional.ofNullable(flowRegistry.get(flowName));
    }

    /**
     * 获取所有已注册的流程名称
     *
     * @return 流程名称列表
     */
    public List<String> getAllFlowNames() {
        return flowRegistry.keySet().stream().toList();
    }

    /**
     * 查找支持当前上下文的 FlowOrchestrator
     *
     * @param context 编排上下文
     * @return 第一个支持的 FlowOrchestrator
     */
    public Optional<FlowOrchestrator> findMatching(FlowContext context) {
        return flowRegistry.values().stream()
                .filter(orchestrator -> orchestrator.supports(context))
                .findFirst();
    }
}
