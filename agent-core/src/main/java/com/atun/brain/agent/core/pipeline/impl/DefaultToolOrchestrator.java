package com.atun.brain.agent.core.pipeline.impl;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.model.AgentResponse;
import com.atun.brain.agent.core.pipeline.FlowContext;
import com.atun.brain.agent.core.pipeline.FlowOrchestrator;
import com.atun.brain.agent.core.pipeline.FlowRegistry;
import com.atun.brain.agent.core.pipeline.ToolOrchestrator;
import com.atun.brain.agent.memory.spi.ChatMemoryProvider;
import com.atun.brain.agent.tools.spi.ToolProvider;
import com.atun.brain.agent.tools.spi.ToolRouteDecision;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.V;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 默认工具编排器实现
 * <p>
 * 基于 AiServices 实现自动工具调用：
 * - DIRECT_LLM：不注册工具，直接对话
 * - DIRECT_TOOL：注册工具，由 LLM 调用指定工具
 * - ORCHESTRATED_FLOW：由 FlowOrchestrator 执行自定义编排流程
 * <p>
 * 设计原则：
 * - 对话场景与工具调用场景使用独立的 systemPrompt
 * - AiServices 实例全局单例复用（工具列表不变）
 * - ChatMemory 按 memoryId 动态绑定
 * - 工具参数信息由 LangChain4j 自动从注解提取，无需手动指定
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class DefaultToolOrchestrator implements ToolOrchestrator {

    private final ChatLanguageModel chatModel;
    private final ChatMemoryProvider memoryProvider;
    private final List<ToolProvider> toolProviders;
    
    private final FlowRegistry flowRegistry;

    /** 全局 AiService 构建器 - 直连 LLM（无工具） */
    private final AiServices<AgentChatService> llmOnlyAiServices;

    /** 全局 AiService 构建器 - 带工具（DIRECT_TOOL） */
    private final AiServices<AgentChatService> toolsEnabledAiServices;

    /**
     * 内部 AiService 接口 - 用于 AiServices 代理生成
     */
    public interface AgentChatService {
        String chat(@V("userId") Long userId, @UserMessage String message);
    }

    public DefaultToolOrchestrator(ChatLanguageModel chatModel,
                                    ChatMemoryProvider memoryProvider,
                                    List<ToolProvider> toolProviders,
                                    String conversationSystemPrompt,
                                    String toolCallSystemPrompt,
                                    FlowRegistry flowRegistry) {
        this.chatModel = chatModel;
        this.memoryProvider = memoryProvider;
        this.toolProviders = toolProviders != null
                ? toolProviders.stream()
                    .filter(ToolProvider::isEnabled)
                    .sorted(Comparator.comparingInt(ToolProvider::getOrder))
                    .toList()
                : List.of();
        this.flowRegistry = flowRegistry;

        // 初始化直连 LLM 的 AiServices（使用对话提示词）
        this.llmOnlyAiServices = createAiServices(false, conversationSystemPrompt);

        // 初始化工具启用的 AiServices（使用工具调用提示词）
        this.toolsEnabledAiServices = createAiServices(true, toolCallSystemPrompt);

        log.info("[DefaultToolOrchestrator] 初始化完成，已注册 {} 个工具组，工具总数：{}",
                this.toolProviders.size(),
                this.toolProviders.stream()
                        .mapToInt(p -> p.getToolObjects().size())
                        .sum());
    }

    /**
     * 创建 AiServices 构建器
     *
     * @param withTools 是否注册工具
     * @param systemPrompt 系统提示词（可为 null 或空）
     * @return AiServices 构建器
     */
    private AiServices<AgentChatService> createAiServices(boolean withTools, String systemPrompt) {
        var builder = AiServices.builder(AgentChatService.class)
                .chatLanguageModel(chatModel);

        // 注入 System Message（如果提供了）
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.systemMessageProvider(context -> systemPrompt);
        }

        // 注册工具 - LangChain4j 会自动从工具方法的注解中提取参数信息
        if (withTools) {
            List<Object> allTools = new ArrayList<>();
            for (ToolProvider provider : toolProviders) {
                allTools.addAll(provider.getToolObjects());
                log.debug("注册工具组：{} ({}个工具)", provider.getGroupName(),
                        provider.getToolObjects().size());
            }
            for (Object tool : allTools) {
                builder.tools(tool);
            }
            log.debug("AiServices 已注册 {} 个工具实例", allTools.size());
        }

        return builder;
    }

    @Override
    public AgentResponse orchestrate(AgentRequest request, ToolRouteDecision decision) {
        long startTime = System.currentTimeMillis();

        try {
            // 根据路由策略选择执行模式
            AgentResponse response = switch (decision.strategy()) {
                case DIRECT_LLM -> executeDirectLlm(request.getMemoryId(), request);
                case DIRECT_TOOL -> executeWithTools(request.getMemoryId(), request);
                case ORCHESTRATED_FLOW -> executeFlow(request, decision.flowName());
            };

            long latencyMs = System.currentTimeMillis() - startTime;
            log.info("[ToolOrchestrator] requestId={}, strategy={}, latencyMs={}",
                    request.getRequestId(), decision.strategy(), latencyMs);

            return response;

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("[ToolOrchestrator] requestId={} 执行失败", request.getRequestId(), e);
            return AgentResponse.error(request.getRequestId(), e.getMessage(), latencyMs);
        }
    }

    /**
     * 直连 LLM（不注册工具）
     * <p>
     * 每次构建时动态绑定指定 memoryId 的 ChatMemory，使用对话系统提示词
     */
    private AgentResponse executeDirectLlm(String memoryId, AgentRequest request) {
        AgentChatService service = llmOnlyAiServices
                .chatMemory(memoryProvider.getMemory(memoryId))
                .build();

        String response = service.chat(request.getUserId(), request.getUserMessage());
        return AgentResponse.simple(request.getRequestId(), response, 0);
    }

    /**
     * 带工具调用（AiServices 自动路由）
     * <p>
     * 每次构建时动态绑定指定 memoryId 的 ChatMemory，使用工具调用系统提示词
     * LangChain4j 会自动将工具信息（名称、描述、参数）注入到 LLM 上下文中
     */
    private AgentResponse executeWithTools(String memoryId, AgentRequest request) {
        AgentChatService service = toolsEnabledAiServices
                .chatMemory(memoryProvider.getMemory(memoryId))
                .build();

        String response = service.chat(request.getUserId(), request.getUserMessage());
        return AgentResponse.withTools(request.getRequestId(), response, List.of("auto"), 0);
    }

    /**
     * 执行编排流程
     * <p>
     * 复杂业务场景由 FlowOrchestrator 自己处理，不使用 systemPrompt
     *
     * @param request  原始请求
     * @param flowName 流程名称
     * @return Agent 响应
     */
    private AgentResponse executeFlow(AgentRequest request, String flowName) {
        if (flowName == null || flowName.isBlank()) {
            throw new IllegalArgumentException("ORCHESTRATED_FLOW 策略必须指定 flowName");
        }

        FlowOrchestrator orchestrator = flowRegistry.getFlowOrchestrator(flowName)
                .orElseThrow(() -> new IllegalArgumentException("未找到流程编排器：" + flowName));

        FlowContext context = FlowContext.builder()
                .request(request)
                .flowName(flowName)
                .build();

        log.info("[FlowOrchestrator] 执行编排流程：flowName={}, requestId={}",
                flowName, request.getRequestId());

        return orchestrator.execute(context);
    }
}
