package com.atun.brain.agent.core.pipeline.router;

import com.atun.brain.agent.core.pipeline.Flow;
import com.atun.brain.agent.core.pipeline.FlowOrchestrator;
import com.atun.brain.agent.core.pipeline.FlowRegistry;
import com.atun.brain.agent.tools.spi.ToolProvider;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 路由上下文构建器
 * <p>
 * 负责从 ToolProvider 和 FlowRegistry 中提取工具和流程的描述信息，
 * 构建 RouteContext 供 RouteDecisionAiService 使用。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Component
public class RouteContextBuilder {

    /**
     * 构建路由上下文
     *
     * @param toolProviders 工具提供者列表
     * @param flowRegistry 流程注册中心
     * @return 路由上下文
     */
    public RouteDecisionAiService.RouteContext build(
            List<ToolProvider> toolProviders,
            FlowRegistry flowRegistry) {

        String[] toolsDesc = extractToolsDescription(toolProviders);
        String[] flowsDesc = extractFlowsDescription(flowRegistry);

        return new RouteDecisionAiService.RouteContext(toolsDesc, flowsDesc);
    }

    /**
     * 从 ToolProvider 中提取工具描述
     * 格式："工具组名 - 工具名：工具描述"
     */
    private String[] extractToolsDescription(List<ToolProvider> toolProviders) {
        if (toolProviders == null || toolProviders.isEmpty()) {
            return new String[0];
        }

        List<String> descriptions = new ArrayList<>();
        for (ToolProvider provider : toolProviders) {
            if (!provider.isEnabled()) {
                continue;
            }
            String groupName = provider.getGroupName();
            for (Object toolObj : provider.getToolObjects()) {
                Class<?> clazz = toolObj.getClass();
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Tool.class)) {
                        Tool toolAnnotation = method.getAnnotation(Tool.class);
                        String toolName = method.getName();
                        String toolDesc = toolAnnotation.value();
                        // 格式：工具组 - 方法名：工具描述
                        descriptions.add(groupName + " - " + toolName + ": " + toolDesc);
                    }
                }
            }
        }
        log.debug("[RouteContextBuilder] 提取到 {} 个工具描述", descriptions.size());
        return descriptions.toArray(new String[0]);
    }

    /**
     * 从 FlowRegistry 中提取流程描述
     * 格式："流程名：流程描述"
     */
    private String[] extractFlowsDescription(FlowRegistry flowRegistry) {
        if (flowRegistry == null) {
            return new String[0];
        }

        List<String> descriptions = new ArrayList<>();
        for (String flowName : flowRegistry.getAllFlowNames()) {
            var orchestratorOpt = flowRegistry.getFlowOrchestrator(flowName);
            if (orchestratorOpt.isPresent()) {
                FlowOrchestrator orchestrator = orchestratorOpt.get();
                Flow flowAnnotation = orchestrator.getClass().getAnnotation(Flow.class);

                String description = orchestrator.getDescription();
                if (flowAnnotation != null && !flowAnnotation.description().isBlank()) {
                    description = flowAnnotation.description();
                }

                // 格式：流程名：流程描述
                descriptions.add(flowName + ": " + description);
            }
        }
        log.debug("[RouteContextBuilder] 提取到 {} 个流程描述", descriptions.size());
        return descriptions.toArray(new String[0]);
    }
}
