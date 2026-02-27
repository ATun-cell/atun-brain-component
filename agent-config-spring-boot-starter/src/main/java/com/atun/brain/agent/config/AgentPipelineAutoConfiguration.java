package com.atun.brain.agent.config;

import com.atun.brain.agent.core.AgentOrchestrator;
import com.atun.brain.agent.core.pipeline.FlowOrchestrator;
import com.atun.brain.agent.core.pipeline.FlowRegistry;
import com.atun.brain.agent.core.pipeline.IntentClassifier;
import com.atun.brain.agent.core.pipeline.MemoryPersister;
import com.atun.brain.agent.core.pipeline.ResponseComposer;
import com.atun.brain.agent.core.pipeline.ToolOrchestrator;
import com.atun.brain.agent.core.pipeline.impl.DefaultIntentClassifier;
import com.atun.brain.agent.core.pipeline.impl.DefaultMemoryPersister;
import com.atun.brain.agent.core.pipeline.impl.DefaultResponseComposer;
import com.atun.brain.agent.core.pipeline.impl.DefaultToolOrchestrator;
import com.atun.brain.agent.memory.spi.ChatMemoryProvider;
import com.atun.brain.agent.rag.spi.RetrievalService;
import com.atun.brain.agent.tools.spi.ToolProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Agent Pipeline 自动配置（支持 FlowOrchestrator）
 * <p>
 * 组装四阶段 Pipeline 并注册 AgentOrchestrator。
 * 支持通过自定义 Bean 替换任意阶段的实现。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentPipelineAutoConfiguration {

    private final AgentProperties agentProperties;
    private final FlowRegistry flowRegistry;

    // ========== Pipeline 阶段 ==========

    @Bean
    @ConditionalOnMissingBean
    public FlowRegistry flowRegistry() {
        return new FlowRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntentClassifier intentClassifier(FlowRegistry flowRegistry) {
        return new DefaultIntentClassifier(flowRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolOrchestrator toolOrchestrator(
            @Qualifier("chatModel") ChatLanguageModel chatModel,
            @Autowired(required = false)
            ChatMemoryProvider memoryProvider,
            @Autowired(required = false)
            List<ToolProvider> toolProviders,
            FlowRegistry flowRegistry) {
        log.info("初始化 ToolOrchestrator: toolProviders={}, conversationSystemPrompt={}, toolCallSystemPrompt={}",
                toolProviders != null ? toolProviders.size() : 0,
                agentProperties.getConversationSystemPrompt() != null ? "已配置" : "未配置",
                agentProperties.getToolCallSystemPrompt() != null ? "已配置" : "未配置");
        return new DefaultToolOrchestrator(chatModel, memoryProvider, toolProviders,
                agentProperties.getConversationSystemPrompt(),
                agentProperties.getToolCallSystemPrompt(),
                flowRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResponseComposer responseComposer(
            @Autowired(required = false)
            RetrievalService retrievalService) {
        boolean ragEnabled = retrievalService != null;
        log.info("初始化 ResponseComposer: ragEnabled={}", ragEnabled);
        return new DefaultResponseComposer(retrievalService);
    }

    @Bean
    @ConditionalOnMissingBean
    public MemoryPersister memoryPersister(ApplicationEventPublisher eventPublisher) {
        return new DefaultMemoryPersister(eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentOrchestrator agentOrchestrator(
            IntentClassifier intentClassifier,
            ToolOrchestrator toolOrchestrator,
            ResponseComposer responseComposer,
            MemoryPersister memoryPersister) {
        log.info("初始化 AgentOrchestrator（四阶段 Pipeline）");
        return new AgentOrchestrator(intentClassifier, toolOrchestrator, responseComposer, memoryPersister);
    }

    /**
     * FlowOrchestrator 自动注册器
     */
    @Bean
    public FlowRegistryInitializer flowRegistryInitializer(FlowRegistry flowRegistry,
                                                            List<FlowOrchestrator> orchestrators) {
        return new FlowRegistryInitializer(flowRegistry, orchestrators);
    }

    /**
     * FlowOrchestrator 注册器内部类
     */
    @RequiredArgsConstructor
    public static class FlowRegistryInitializer implements InitializingBean {
        private final FlowRegistry flowRegistry;
        private final List<FlowOrchestrator> orchestrators;

        @Override
        public void afterPropertiesSet() {
            for (FlowOrchestrator orchestrator : orchestrators) {
                flowRegistry.register(orchestrator);
            }
            log.info("[FlowRegistryInitializer] 已注册 {} 个 FlowOrchestrator", orchestrators.size());
        }
    }
}
