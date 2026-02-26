package com.atun.brain.agent.config;

import com.atun.brain.agent.config.AgentProperties.ModelProviderConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 模型自动配置
 * <p>
 * 条件化注册 ChatModel / EmbeddingModel Bean。
 * 支持通过 agent.provider 切换 dashscope / openai 提供商。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AgentProperties.class)
public class AgentModelAutoConfiguration {
    
    private final AgentProperties agentProperties;
    
    /**
     * 对话模型（日常对话，快速响应）
     */
    @Bean("chatModel")
    @ConditionalOnMissingBean(name = "chatModel")
    public ChatLanguageModel chatModel() {
        return buildChatModel("chat", null, null);
    }
    
    /**
     * 分析模型（财务分析，平衡性能）
     */
    @Bean("analysisModel")
    @ConditionalOnMissingBean(name = "analysisModel")
    public ChatLanguageModel analysisModel() {
        return buildChatModel("analysis", null, null);
    }
    
    /**
     * 报告生成模型（高质量输出）
     */
    @Bean("reportModel")
    @ConditionalOnMissingBean(name = "reportModel")
    public ChatLanguageModel reportModel() {
        return buildChatModel("report", null, null);
    }
    
    /**
     * 交易解析模型（结构化输出，低温度提高准确性）
     */
    @Bean("parsingModel")
    @ConditionalOnMissingBean(name = "parsingModel")
    public ChatLanguageModel parsingModel() {
        String modelName = agentProperties.getModel("parsing");
        if (modelName == null) {
            modelName = agentProperties.getModel("analysis"); // 回退到分析模型
        }
        return buildChatModel("parsing", modelName, 0.1);
    }
    
    /**
     * 向量化模型
     */
    @Bean("embeddingModel")
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel() {
        ModelProviderConfig config = agentProperties.getCurrentProvider();
        String modelName = agentProperties.getModel("embedding");
        
        log.info("初始化向量嵌入模型: provider={}, model={}",
                agentProperties.getProvider(), modelName);
        
        return OpenAiEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(modelName)
                .timeout(Duration.ofSeconds(config.getTimeout()))
                .maxRetries(agentProperties.getRetry().getMaxAttempts())
                .logRequests(true)
                .logResponses(true)
                .build();
    }
    
    // ========== 内部方法 ==========
    
    private ChatLanguageModel buildChatModel(String scenario, String overrideModel, Double overrideTemperature) {
        ModelProviderConfig config = agentProperties.getCurrentProvider();
        String modelName = overrideModel != null ? overrideModel : agentProperties.getModel(scenario);
        double temperature = overrideTemperature != null ? overrideTemperature : config.getTemperature();
        
        log.info("初始化模型[{}]: provider={}, model={}, temperature={}",
                scenario, agentProperties.getProvider(), modelName, temperature);
        
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(config.getMaxTokens())
                .timeout(Duration.ofSeconds(config.getTimeout()))
                .maxRetries(agentProperties.getRetry().getMaxAttempts())
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
