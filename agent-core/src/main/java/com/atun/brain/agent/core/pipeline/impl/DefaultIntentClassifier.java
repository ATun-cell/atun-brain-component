package com.atun.brain.agent.core.pipeline.impl;

import com.atun.brain.agent.core.model.AgentRequest;
import com.atun.brain.agent.core.pipeline.IntentClassifier;
import com.atun.brain.agent.tools.spi.ToolRouteDecision;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 默认意图分类器实现
 * <p>
 * 基于关键词规则的轻量级分类，避免对简单问候/闲聊类请求触发工具调用。
 * 复杂场景统一走 AUTO_TOOL_CHAIN 由 AiServices 自动判断。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class DefaultIntentClassifier implements IntentClassifier {
    
    /** 闲聊/问候关键词集合 */
    private static final Set<String> CHITCHAT_KEYWORDS = Set.of(
            "你好", "嗨", "hello", "hi", "你是谁", "你叫什么",
            "谢谢", "再见", "拜拜", "辛苦了", "好的"
    );
    
    /** 工具触发高概率关键词模式 */
    private static final Pattern TOOL_TRIGGER_PATTERN = Pattern.compile(
            ".*(记账|记一笔|花了|收入|支出|消费|查询|统计|分类|创建|删除|搜索|推荐|分析|报告|预算|" +
            "这个月|上个月|本周|昨天|今天|多少钱|花费|总共|汇总|账单).*"
    );
    
    @Override
    public ToolRouteDecision classify(AgentRequest request) {
        String message = request.getUserMessage().trim();
        
        // 1. 短消息闲聊检测
        if (message.length() <= 10 && isChitChat(message)) {
            log.debug("[IntentClassifier] requestId={} → DIRECT_LLM (闲聊)",
                    request.getRequestId());
            return ToolRouteDecision.directLlm("用户消息为简短闲聊/问候");
        }
        
        // 2. 工具关键词匹配
        if (TOOL_TRIGGER_PATTERN.matcher(message).matches()) {
            log.debug("[IntentClassifier] requestId={} → AUTO_TOOL_CHAIN (命中工具关键词)",
                    request.getRequestId());
            return ToolRouteDecision.autoToolChain("消息包含财务操作关键词，交由 AiServices 自动路由");
        }
        
        // 3. 默认：自动工具链（让 LLM 自行决定是否需要工具）
        log.debug("[IntentClassifier] requestId={} → AUTO_TOOL_CHAIN (默认策略)",
                request.getRequestId());
        return ToolRouteDecision.autoToolChain("默认策略，由 AiServices 自动判断");
    }
    
    private boolean isChitChat(String message) {
        String normalized = message.toLowerCase().replaceAll("[！？。，、!?,.]", "");
        return CHITCHAT_KEYWORDS.stream().anyMatch(normalized::contains);
    }
}
