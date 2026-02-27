package com.atun.brain.agent.core.pipeline.router;

import com.atun.brain.agent.tools.spi.ToolRouteDecision;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 默认智能路由决策 AI 服务实现
 * <p>
 * 基于 LangChain4j AiServices 实现，通过 LLM 理解用户意图，
 * 结合可用的工具列表和流程编排器列表，智能判断路由策略。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Service
public class DefaultRouteDecisionAiService implements RouteDecisionAiService {

    private final RouteDecisionService routeDecisionService;

    /**
     * 内部 AiService 接口定义
     */
    private interface RouteDecisionService {
        @SystemMessage("""
                你是一个智能路由决策助手。你的任务是根据用户输入和可用的工具/流程列表，判断用户意图应该走哪种路由策略。

                可用的路由策略有三种：
                1. DIRECT_LLM - 直接调用 LLM 回答，适用于：简短闲聊、问候、告别、感谢等社交性对话，不需要任何工具或流程
                2. DIRECT_TOOL - 直接调用指定工具，适用于：用户明确指定要使用某个特定单一工具的场景
                3. ORCHESTRATED_FLOW - 执行编排流程，适用于：需要使用工具、多步骤操作、复杂业务场景

                决策说明：
                - 仔细分析用户输入的意图，结合可用工具列表和流程编排器列表进行匹配
                - 如果用户输入涉及工具使用（如查询、创建、删除、统计等操作），优先匹配流程编排器
                - 如果没有匹配的流程编排器，但明确指向某个单一工具，返回 DIRECT_TOOL
                - 如果是简单对话、闲聊、问候，返回 DIRECT_LLM

                可用的工具列表：
                {{tools}}

                可用的流程编排器列表：
                {{flows}}

                请只返回 JSON 格式的路由决策，不要返回其他内容。JSON 格式如下：
                {
                  "strategy": "策略名称",
                  "reason": "决策原因说明",
                  "flowName": "流程名称（仅当 strategy 为 ORCHESTRATED_FLOW 时填写）",
                  "toolName": "工具名称（仅当 strategy 为 DIRECT_TOOL 时填写）"
                }

                示例 1（闲聊）：
                {"strategy": "DIRECT_LLM", "reason": "用户输入为简短问候"}

                示例 2（工具调用）：
                {"strategy": "DIRECT_TOOL", "toolName": "weatherTool", "reason": "用户需要查询天气"}

                示例 3（流程编排）：
                {"strategy": "ORCHESTRATED_FLOW", "flowName": "accountingFlow", "reason": "用户需要记账"}
                """)
        String decide(@V("userId") String userId,
                      @V("tools") String tools,
                      @V("flows") String flows,
                      @UserMessage String userMessage);
    }

    public DefaultRouteDecisionAiService(@Qualifier("chatModel") ChatLanguageModel chatModel) {
        this.routeDecisionService = AiServices.builder(RouteDecisionService.class)
                .chatLanguageModel(chatModel)
                .build();
    }

    @Override
    public ToolRouteDecision decide(String userMessage, RouteContext context) {
        try {
            // 构建工具和流程的描述字符串
            String toolsDesc = formatToolsDescription(context.availableTools());
            String flowsDesc = formatFlowsDescription(context.availableFlows());

            // 调用 AiService 进行智能路由判断
            String decisionJson = routeDecisionService.decide(
                    "router-" + System.currentTimeMillis(),
                    toolsDesc,
                    flowsDesc,
                    userMessage
            );

            // 解析 LLM 返回的决策
            return parseDecision(decisionJson, context);

        } catch (Exception e) {
            log.error("[DefaultRouteDecisionAiService] 智能路由决策失败，降级为 DIRECT_LLM", e);
            // 降级策略：默认走直连 LLM（最安全的降级方式）
            return ToolRouteDecision.directLlm("智能路由决策异常，降级为直连 LLM");
        }
    }

    /**
     * 格式化工具描述
     */
    private String formatToolsDescription(String[] availableTools) {
        if (availableTools == null || availableTools.length == 0) {
            return "无可用工具";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < availableTools.length; i++) {
            sb.append(i + 1).append(". ").append(availableTools[i]).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 格式化流程描述
     */
    private String formatFlowsDescription(String[] availableFlows) {
        if (availableFlows == null || availableFlows.length == 0) {
            return "无可用流程编排器";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < availableFlows.length; i++) {
            sb.append(i + 1).append(". ").append(availableFlows[i]).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 解析 LLM 返回的决策
     * 预期的 JSON 格式：
     * {"strategy": "ORCHESTRATED_FLOW", "flowName": "...", "reason": "..."}
     * {"strategy": "DIRECT_TOOL", "toolName": "...", "reason": "..."}
     * {"strategy": "DIRECT_LLM", "reason": "..."}
     */
    private ToolRouteDecision parseDecision(String decisionJson, RouteContext context) {
        // 提取 JSON 内容（可能包含 markdown 代码块）
        String jsonContent = extractJsonContent(decisionJson);

        // 解析 JSON 字段
        String strategy = extractJsonValue(jsonContent, "strategy");
        String reason = extractJsonValue(jsonContent, "reason");
        String flowName = extractJsonValue(jsonContent, "flowName");
        String toolName = extractJsonValue(jsonContent, "toolName");

        if (strategy == null) {
            log.warn("[DefaultRouteDecisionAiService] 无法解析策略，降级为 DIRECT_LLM. JSON: {}", decisionJson);
            return ToolRouteDecision.directLlm("无法解析 LLM 决策");
        }

        return switch (strategy.toUpperCase()) {
            case "DIRECT_LLM" -> ToolRouteDecision.directLlm(reason != null ? reason : "用户输入为闲聊或简单问答");
            case "DIRECT_TOOL" -> {
                if (toolName == null) {
                    log.warn("[DefaultRouteDecisionAiService] DIRECT_TOOL 策略未指定 toolName，降级为 DIRECT_LLM");
                    yield ToolRouteDecision.directLlm("DIRECT_TOOL 未指定工具名");
                }
                yield ToolRouteDecision.directTool(toolName, reason != null ? reason : "用户明确指定使用工具：" + toolName);
            }
            case "ORCHESTRATED_FLOW" -> {
                if (flowName == null) {
                    log.warn("[DefaultRouteDecisionAiService] ORCHESTRATED_FLOW 策略未指定 flowName，降级为 DIRECT_LLM");
                    yield ToolRouteDecision.directLlm("ORCHESTRATED_FLOW 未指定流程名");
                }
                // 验证 flowName 是否有效
                boolean flowExists = context.availableFlows() != null &&
                        java.util.Arrays.stream(context.availableFlows())
                                .anyMatch(f -> f.contains(flowName) || flowName.contains(f));
                if (!flowExists) {
                    log.warn("[DefaultRouteDecisionAiService] ORCHESTRATED_FLOW 指定的 flowName={} 不存在，降级为 DIRECT_LLM", flowName);
                    yield ToolRouteDecision.directLlm("指定的流程编排器不存在：" + flowName);
                }
                yield ToolRouteDecision.orchestratedFlow(flowName, reason != null ? reason : "匹配流程编排器：" + flowName);
            }
            default -> {
                log.warn("[DefaultRouteDecisionAiService] 未知策略：{}，降级为 DIRECT_LLM", strategy);
                yield ToolRouteDecision.directLlm("未知策略：" + strategy);
            }
        };
    }

    /**
     * 从响应中提取纯 JSON 内容
     * 处理可能包含 markdown 代码块的情况，如 ```json {...} ```
     */
    private String extractJsonContent(String response) {
        if (response == null || response.isBlank()) {
            return "{}";
        }

        String content = response.trim();

        // 处理 markdown 代码块
        if (content.contains("```")) {
            int start = content.indexOf("```");
            // 跳过可能的语言标识符（如 ```json）
            int newlinePos = content.indexOf("\n", start);
            if (newlinePos != -1) {
                start = newlinePos + 1;
            }
            int end = content.indexOf("```", start);
            if (end != -1) {
                content = content.substring(start, end).trim();
            }
        }

        // 找到第一个 { 和最后一个 }
        int startBrace = content.indexOf('{');
        int endBrace = content.lastIndexOf('}');

        if (startBrace != -1 && endBrace != -1 && endBrace > startBrace) {
            return content.substring(startBrace, endBrace + 1);
        }

        return content;
    }

    /**
     * 简单 JSON 值提取（不依赖外部 JSON 库）
     * 支持提取字符串值和简单非字符串值
     */
    private String extractJsonValue(String json, String key) {
        if (json == null || json.isBlank()) {
            return null;
        }

        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int startIndex = colonIndex + 1;
        // 跳过空白字符
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }

        if (startIndex >= json.length()) {
            return null;
        }

        // 判断是字符串还是其他类型
        char firstChar = json.charAt(startIndex);
        if (firstChar == '"') {
            // 字符串值 - 需要处理转义字符
            StringBuilder value = new StringBuilder();
            int i = startIndex + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\') {
                    // 处理转义字符
                    i++;
                    if (i < json.length()) {
                        char escaped = json.charAt(i);
                        switch (escaped) {
                            case 'n' -> value.append('\n');
                            case 't' -> value.append('\t');
                            case 'r' -> value.append('\r');
                            case '"' -> value.append('"');
                            case '\\' -> value.append('\\');
                            default -> value.append(escaped);
                        }
                    }
                } else if (c == '"') {
                    // 字符串结束
                    break;
                } else {
                    value.append(c);
                }
                i++;
            }
            return value.toString();
        } else if (firstChar == 'n' && json.startsWith("null", startIndex)) {
            // null 值
            return null;
        } else {
            // 非字符串值（如数字、布尔值）
            int endIndex = startIndex;
            while (endIndex < json.length() && json.charAt(endIndex) != ',' && json.charAt(endIndex) != '}') {
                endIndex++;
            }
            String value = json.substring(startIndex, endIndex).trim();
            return value.isEmpty() || value.equals("null") ? null : value;
        }
    }
}