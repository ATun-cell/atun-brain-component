package com.atun.brain.agent.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 层自动配置
 * <p>
 * 本配置类为标记配置，核心接口 {@link com.atun.brain.agent.rag.spi.EmbeddingStoreProvider}
 * 和 {@link com.atun.brain.agent.rag.spi.RetrievalService} 需由应用服务自行实现。
 * <p>
 * 使用方式：
 * 1. 应用实现 EmbeddingStoreProvider 和 RetrievalService 接口（或提供 {@code @Bean}）
 * 2. 框架自动注入并使用
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "agent.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentRagAutoConfiguration {

    private final AgentProperties agentProperties;
}
