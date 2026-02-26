package com.atun.brain.agent.config;

import com.atun.brain.agent.memory.spi.ChatMemoryProvider;
import com.atun.brain.agent.memory.spi.MemoryWindowConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 记忆层自动配置
 * <p>
 * 本配置类仅提供记忆窗口配置，核心接口 {@link ChatMemoryProvider} 需由应用服务自行实现。
 * <p>
 * 使用方式：
 * 1. 应用实现 {@link ChatMemoryProvider} 接口（或提供 {@code @Bean}）
 * 2. 框架自动注入并使用
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "agent.memory", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentMemoryAutoConfiguration {

    private final AgentProperties agentProperties;

    /**
     * 记忆窗口配置
     * <p>
     * 根据配置决定使用 token 窗口还是消息窗口策略。
     *
     * @return MemoryWindowConfig 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MemoryWindowConfig memoryWindowConfig() {
        AgentProperties.MemoryConfig memoryConfig = agentProperties.getMemory();

        log.info("初始化记忆窗口配置：strategy={}, maxTokens={}, maxMessages={}",
                memoryConfig.getStrategy(),
                memoryConfig.getMaxTokens(),
                memoryConfig.getMaxMessages());

        return switch (memoryConfig.getStrategy().toLowerCase()) {
            case "token" -> MemoryWindowConfig.tokenWindow(memoryConfig.getMaxTokens());
            default -> MemoryWindowConfig.messageWindow(memoryConfig.getMaxMessages());
        };
    }
}
