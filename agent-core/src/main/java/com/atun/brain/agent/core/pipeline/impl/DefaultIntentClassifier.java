package com.atun.brain.agent.core.pipeline.impl;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.pipeline.Flow;
import com.atun.brain.agent.core.pipeline.FlowOrchestrator;
import com.atun.brain.agent.core.pipeline.FlowRegistry;
import com.atun.brain.agent.core.pipeline.IntentClassifier;
import com.atun.brain.agent.core.pipeline.router.RouteContextBuilder;
import com.atun.brain.agent.core.pipeline.router.RouteDecisionAiService;
import com.atun.brain.agent.tools.spi.ToolProvider;
import com.atun.brain.agent.tools.spi.ToolRouteDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 默认意图分类器（支持 FlowOrchestrator）
 * <p>
 * 采用两级分类策略：
 * 1. 规则层：基于关键词的快速检测，处理闲聊/问候等简单场景
 * 2. AI 层：基于 LLM 的智能路由，理解用户意图并匹配最合适的工具或流程编排器
 * <p>
 * 优势：
 * - 保留规则检测用于快速处理闲聊，避免不必要的 LLM 调用开销
 * - 通过 AI 服务智能判断用户意图，提高输入自由度，无需用户明确指定流程关键词
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Component
public class DefaultIntentClassifier implements IntentClassifier {

    private final FlowRegistry flowRegistry;
    private final List<ToolProvider> toolProviders;
    private final RouteDecisionAiService routeDecisionAiService;
    private final RouteContextBuilder routeContextBuilder;

    /** 闲聊/问候关键词集合（规则层快速检测） */
    private static final Set<String> CHITCHAT_KEYWORDS = Set.of(
            "你好", "嗨", "hello", "hi", "你是谁", "你叫什么",
            "谢谢", "再见", "拜拜", "辛苦了", "好的", "早上好", "下午好", "晚上好"
    );

    /** 明确指示走流程编排的关键词（规则层快速检测） */
    private static final Pattern FLOW_EXPLICIT_PATTERN = Pattern.compile(
            ".*(开始流程 | 执行流程 | 走流程 | 运行流程 | 进入.*流程 | 帮我.*分析 | 帮我.*审批).*"
    );

    public DefaultIntentClassifier(FlowRegistry flowRegistry,
                                    List<ToolProvider> toolProviders,
                                    RouteDecisionAiService routeDecisionAiService,
                                    RouteContextBuilder routeContextBuilder) {
        this.flowRegistry = flowRegistry;
        this.toolProviders = toolProviders != null ? toolProviders : List.of();
        this.routeDecisionAiService = routeDecisionAiService;
        this.routeContextBuilder = routeContextBuilder;
    }

    @Override
    public ToolRouteDecision classify(AgentRequest request) {
        String message = request.getUserMessage().trim();
        String requestId = request.getRequestId();

        // ========== 规则层：快速检测 ==========

        // 1. 短消息闲聊检测（优先处理，避免 LLM 调用开销）
        if (message.length() <= 10 && isChitChat(message)) {
            log.debug("[IntentClassifier] requestId={} -> DIRECT_LLM (闲聊)", requestId);
            return ToolRouteDecision.directLlm("用户消息为简短闲聊/问候");
        }

        // 2. 明确指示走流程编排的关键词检测
        if (FLOW_EXPLICIT_PATTERN.matcher(message).matches()) {
            // 尝试从消息中提取流程名称
            ToolRouteDecision explicitFlowDecision = matchExplicitFlow(message);
            if (explicitFlowDecision != null) {
                log.debug("[IntentClassifier] requestId={} -> ORCHESTRATED_FLOW (明确流程指示：{})",
                        requestId, explicitFlowDecision.flowName());
                return explicitFlowDecision;
            }
        }

        // ========== AI 层：智能路由判断 ==========

        // 3. 构建路由上下文（包含所有可用工具和流程编排器信息）
        var context = routeContextBuilder.build(toolProviders, flowRegistry);

        log.debug("[IntentClassifier] requestId={} -> AI_ROUTE (可用工具：{} 个，可用流程：{} 个)",
                requestId, context.availableTools().length, context.availableFlows().length);

        // 4. 调用 AI 服务进行智能路由判断
        ToolRouteDecision aiDecision = routeDecisionAiService.decide(message, context);

        log.info("[IntentClassifier] requestId={} -> {} (AI 路由决策，原因：{})",
                requestId, aiDecision.strategy(), aiDecision.reason());

        return aiDecision;
    }

    /**
     * 匹配明确指示走流程编排的关键词
     * 例如："帮我分析上个月的财务状况" -> 匹配 "分析" + "财务"
     */
    private ToolRouteDecision matchExplicitFlow(String message) {
        for (String flowName : flowRegistry.getAllFlowNames()) {
            var orchestratorOpt = flowRegistry.getFlowOrchestrator(flowName);
            if (orchestratorOpt.isPresent()) {
                FlowOrchestrator orchestrator = orchestratorOpt.get();
                Flow flowAnnotation = orchestrator.getClass().getAnnotation(Flow.class);
                if (flowAnnotation != null) {
                    // 检查触发关键词
                    for (String keyword : flowAnnotation.triggerKeywords()) {
                        if (message.contains(keyword)) {
                            return ToolRouteDecision.orchestratedFlow(
                                    flowName,
                                    "命中流程 [" + flowName + "] 的触发关键词：" + keyword
                            );
                        }
                    }
                    // 检查流程描述中的关键词是否在消息中出现
                    String description = flowAnnotation.description();
                    if (description != null && !description.isBlank()) {
                        // 提取描述中的关键名词（简单实现：按空格和标点分割）
                        String[] descWords = description.split("[\\s,，.。]+");
                        for (String word : descWords) {
                            if (word.length() >= 2 && message.contains(word)) {
                                return ToolRouteDecision.orchestratedFlow(
                                        flowName,
                                        "命中流程 [" + flowName + "] 的描述关键词：" + word
                                );
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isChitChat(String message) {
        String normalized = message.toLowerCase().replaceAll("[!?,.，。]", "");
        return CHITCHAT_KEYWORDS.stream().anyMatch(normalized::contains);
    }
}
