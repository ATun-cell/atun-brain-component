package com.atun.brain.agent.tools.spi;

import java.util.List;

/**
 * 工具提供者 SPI - 用于注册和发现 Agent 工具
 * <p>
 * 实现类通过 Spring Bean 注册，由 agent-core 自动扫描并注入到 AiServices。
 * 新增工具仅需实现此接口并标注 @Component。
 *
 * @author lij
 * @since 1.0
 */
public interface ToolProvider {
    
    /**
     * 获取工具组名称（唯一标识）
     *
     * @return 工具组名称，如 "finance-tools"、"calendar-tools"
     */
    String getGroupName();
    
    /**
     * 获取工具组描述
     *
     * @return 工具组功能描述
     */
    String getDescription();
    
    /**
     * 获取此工具组提供的所有工具实例
     * <p>
     * 返回的对象中包含 @Tool 注解的方法，将被注册到 AiServices。
     *
     * @return 包含 @Tool 方法的对象列表
     */
    List<Object> getToolObjects();
    
    /**
     * 工具组排序（数值越小优先级越高）
     *
     * @return 排序值，默认 100
     */
    default int getOrder() {
        return 100;
    }
    
    /**
     * 是否启用此工具组
     *
     * @return true 启用，false 禁用
     */
    default boolean isEnabled() {
        return true;
    }
}
