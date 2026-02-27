package com.atun.brain.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Agent 统一配置属性
 * <p>
 * 整合 AI 模型、工具、记忆、RAG 的全链路配置。
 * 配置前缀：agent.*
 *
 * @author lij
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
    
    // ==================== AI 模型配置 ====================

    /** AI 服务提供商：dashscope | openai */
    private String provider = "dashscope";

    /** 阿里云百炼配置 */
    private ModelProviderConfig dashscope = new ModelProviderConfig();
    
    /** OpenAI 配置 */
    private ModelProviderConfig openai = new ModelProviderConfig();
    
    /** 重试配置 */
    private RetryConfig retry = new RetryConfig();
    
    // ==================== 记忆配置 ====================
    
    /** 记忆配置 */
    private MemoryConfig memory = new MemoryConfig();
    
    // ==================== RAG 配置 ====================
    
    /** RAG 配置 */
    private RagConfig rag = new RagConfig();
    
    // ==================== 工具配置 ====================
    
    /** 工具配置 */
    private ToolsConfig tools = new ToolsConfig();
    
    // ==================== 内部类 ====================
    
    @Data
    public static class ModelProviderConfig {
        private String apiKey;
        private String baseUrl;
        private Map<String, String> models;
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
        private Integer timeout = 60;
    }
    
    @Data
    public static class RetryConfig {
        private Integer maxAttempts = 3;
        private Long backoffMs = 1000L;
    }
    
    @Data
    public static class MemoryConfig {
        /** 是否启用对话记忆 */
        private boolean enabled = true;
        /** 窗口策略：message | token */
        private String strategy = "message";
        /** 最大消息数（message 策略） */
        private int maxMessages = 20;
        /** 最大 token 数（token 策略） */
        private int maxTokens = 4096;
    }

    @Data
    public static class RagConfig {
        /** 是否启用 RAG */
        private boolean enabled = true;
    }
    
    @Data
    public static class ToolsConfig {
        /** 是否启用内置工具 */
        private boolean enabled = true;
        /** 启用的工具组列表（空=全部启用） */
        private java.util.List<String> enabledGroups = java.util.List.of();
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 获取当前活跃的提供商配置
     */
    public ModelProviderConfig getCurrentProvider() {
        return switch (provider.toLowerCase()) {
            case "openai" -> openai;
            case "dashscope" -> dashscope;
            default -> dashscope;
        };
    }
    
    /**
     * 获取指定场景的模型名称
     */
    public String getModel(String scenario) {
        ModelProviderConfig config = getCurrentProvider();
        if (config.getModels() == null || !config.getModels().containsKey(scenario)) {
            return config.getModels() != null ? config.getModels().get("default") : "qwen-turbo";
        }
        return config.getModels().get(scenario);
    }
}
