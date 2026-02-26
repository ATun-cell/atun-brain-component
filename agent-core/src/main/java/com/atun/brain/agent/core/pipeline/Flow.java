package com.atun.brain.agent.core.pipeline;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 流程编排器标识注解
 * <p>
 * 标注在 FlowOrchestrator 实现类上，用于声明流程元数据。
 * 支持 Spring 自动扫描和注册到 FlowRegistry。
 *
 * @author lij
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Flow {

    /**
     * 流程名称（唯一标识）
     * <p>
     * 必须与 ToolRouteDecision.flowName 匹配才能触发此编排器
     *
     * @return 流程名称
     */
    String name();

    /**
     * 流程描述
     * <p>
     * 用于 IntentClassifier 决策参考
     *
     * @return 流程描述
     */
    String description() default "";

    /**
     * 流程版本（用于灰度/多版本管理）
     *
     * @return 版本号，默认 "1.0"
     */
    String version() default "1.0";

    /**
     * 是否启用此流程
     *
     * @return true 启用，默认 true
     */
    boolean enabled() default true;

    /**
     * 流程触发关键词（可选，用于 IntentClassifier 快速匹配）
     *
     * @return 关键词数组
     */
    String[] triggerKeywords() default {};
}
