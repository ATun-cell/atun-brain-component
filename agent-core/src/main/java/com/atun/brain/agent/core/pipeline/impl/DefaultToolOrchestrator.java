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
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 默认工具编排器实现
 * <p>
 * 基于 AiServices 实现自动工具调用：
 * - DIRECT_LLM：不注册工具，直接对话
 * - DIRECT_TOOL：注册工具，由 LLM 调用指定工具
 * - ORCHESTRATED_FLOW：由 FlowOrchestrator 执行自定义编排流程
 * <p>
 * 设计原则：
 * - 对话场景与工具调用场景使用独立的 systemPrompt（写死在代码中）
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
    private final AiServices<AgentChatServiceWithTools> toolsEnabledAiServices;

    /**
     * 内部 AiService 接口 - 直连 LLM
     */
    public interface AgentChatService {
        @SystemMessage("你是一个专业、友好、乐于助人的 AI 助手。\n" +
                "\n" +
                "请遵循以下原则：\n" +
                "1. 用简洁清晰的语言回答问题\n" +
                "2. 保持礼貌和专业的语气\n" +
                "3. 如果遇到不确定的问题，如实告知用户\n" +
                "4. 对于复杂问题，分步骤解释说明\n")
        String chat(@V("userId") Long userId, @UserMessage String message);
    }

    /**
     * 内部 AiService 接口 - 带工具调用
     */
    public interface AgentChatServiceWithTools {
        @SystemMessage("你是一个智能助手，可以调用工具来帮助用户完成任务。\n" +
                "\n" +
                "工具调用原则：\n" +
                "1. 分析用户请求，判断是否需要使用工具\n" +
                "2. 如果需要工具，选择最合适的工具并提取必要的参数\n" +
                "3. 如果单个工具无法完成任务，可以按顺序调用多个工具\n" +
                "4. 工具调用后，根据返回结果组织自然语言回复用户\n" +
                "\n" +
                "可用工具已注册到系统中，你可以根据工具描述和参数说明进行选择。\n")
        String chat(@V("userId") Long userId, @UserMessage String message);
    }

    public DefaultToolOrchestrator(ChatLanguageModel chatModel,
                                   ChatMemoryProvider memoryProvider,
                                   List<ToolProvider> toolProviders,
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

        // 初始化直连 LLM 的 AiServices
        this.llmOnlyAiServices = AiServices.builder(AgentChatService.class)
                .chatLanguageModel(chatModel)
                .build();

        // 初始化工具启用的 AiServices
        var toolsBuilder = AiServices.builder(AgentChatServiceWithTools.class)
                .chatLanguageModel(chatModel);

        // 注册工具
        List<Object> allTools = new ArrayList<>();
        for (ToolProvider provider : this.toolProviders) {
            allTools.addAll(provider.getToolObjects());
        }
        for (Object tool : allTools) {
            toolsBuilder.tools(tool);
        }
        this.toolsEnabledAiServices = toolsBuilder.build();

        log.info("[DefaultToolOrchestrator] 初始化完成，已注册 {} 个工具组，工具总数：{}",
                this.toolProviders.size(),
                this.toolProviders.stream()
                        .mapToInt(p -> p.getToolObjects().size())
                        .sum());
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
     * 每次构建时动态绑定指定 memoryId 的 ChatMemory
     */
    private AgentResponse executeDirectLlm(String memoryId, AgentRequest request) {
        AgentChatService service = llmOnlyAiServices
                .chatMemory(memoryProvider.getMemory(memoryId))
                .build();

        String response = service.chat(request.getUserId(), request.getUserMessage());
        return AgentResponse.simple(request.getRequestId(), response, 0);
    }

    /**
     * 带工具调用
     * <p>
     * 每次构建时动态绑定指定 memoryId 的 ChatMemory
     * LangChain4j 会自动将工具信息（名称、描述、参数）注入到 LLM 上下文中
     */
    private AgentResponse executeWithTools(String memoryId, AgentRequest request) {
        AgentChatServiceWithTools service = toolsEnabledAiServices
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
