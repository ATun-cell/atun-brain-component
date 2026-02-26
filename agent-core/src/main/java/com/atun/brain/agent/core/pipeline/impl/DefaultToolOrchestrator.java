package com.atun.brain.agent.core.pipeline.impl;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.model.AgentResponse;
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

/**
 * 默认工具编排器实现
 * <p>
 * 基于 AiServices 实现自动工具调用：
 * - DIRECT_LLM：不注册工具，直接对话
 * - AUTO_TOOL_CHAIN：注册所有工具，由 LLM 自动决策
 *
 * 设计原则：
 * - AiServices 实例全局单例复用（工具列表不变）
 * - ChatMemory 按 memoryId 动态绑定
 * - System Message 可外部注入
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class DefaultToolOrchestrator implements ToolOrchestrator {

    private final ChatLanguageModel chatModel;
    private final ChatMemoryProvider memoryProvider;
    private final List<ToolProvider> toolProviders;
    private final String systemPrompt;

    /** 全局 AiService 构建器 - 直连 LLM（无工具） */
    private final AiServices<AgentChatService> llmOnlyAiServices;

    /** 全局 AiService 构建器 - 带工具（AUTO_TOOL_CHAIN） */
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
                                    String systemPrompt) {
        this.chatModel = chatModel;
        this.memoryProvider = memoryProvider;
        this.toolProviders = toolProviders != null
                ? toolProviders.stream()
                    .filter(ToolProvider::isEnabled)
                    .sorted(Comparator.comparingInt(ToolProvider::getOrder))
                    .toList()
                : List.of();
        this.systemPrompt = systemPrompt;

        // 初始化直连 LLM 的 AiServices（无工具）
        this.llmOnlyAiServices = createAiServices(false);

        // 初始化工具启用的 AiServices
        this.toolsEnabledAiServices = createAiServices(true);

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
     * @return AiServices 构建器
     */
    private AiServices<AgentChatService> createAiServices(boolean withTools) {
        var builder = AiServices.builder(AgentChatService.class)
                .chatLanguageModel(chatModel);

        // 注入 System Message（如果提供了）
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            // 使用 systemMessageProvider 注入固定的系统消息
            builder.systemMessageProvider(context -> systemPrompt);
        }

        // 注册工具
        if (withTools) {
            List<Object> allTools = new ArrayList<>();
            for (ToolProvider provider : toolProviders) {
                allTools.addAll(provider.getToolObjects());
                log.info("注册工具组：{} ({}个工具)", provider.getGroupName(),
                        provider.getToolObjects().size());
            }
            for (Object tool : allTools) {
                builder.tools(tool);
            }
        }

        return builder;
    }

    @Override
    public AgentResponse orchestrate(AgentRequest request, ToolRouteDecision decision) {
        long startTime = System.currentTimeMillis();

        try {
            String memoryId = request.getMemoryId();

            // 根据路由策略选择执行模式
            String response = switch (decision.strategy()) {
                case DIRECT_LLM -> executeDirectLlm(memoryId, request);
                case DIRECT_TOOL, AUTO_TOOL_CHAIN -> executeWithTools(memoryId, request);
            };

            long latencyMs = System.currentTimeMillis() - startTime;
            log.info("[ToolOrchestrator] requestId={}, strategy={}, latencyMs={}",
                    request.getRequestId(), decision.strategy(), latencyMs);

            return AgentResponse.withTools(request.getRequestId(), response, List.of("auto"), latencyMs);

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
    private String executeDirectLlm(String memoryId, AgentRequest request) {
        AgentChatService service = llmOnlyAiServices
                .chatMemory(memoryProvider.getMemory(memoryId))
                .build();

        return service.chat(request.getUserId(), request.getUserMessage());
    }

    /**
     * 带工具调用（AiServices 自动路由）
     * <p>
     * 每次构建时动态绑定指定 memoryId 的 ChatMemory
     */
    private String executeWithTools(String memoryId, AgentRequest request) {
        AgentChatService service = toolsEnabledAiServices
                .chatMemory(memoryProvider.getMemory(memoryId))
                .build();

        return service.chat(request.getUserId(), request.getUserMessage());
    }
}
