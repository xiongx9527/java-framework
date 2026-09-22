package com.xiongx9527.fw.plus.ai.mcp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP 工具便捷模式注解
 *
 * <p>对齐 spec 7.2：标注在 Spring Bean 的 public 方法上，启动时被扫描注册为 MCP 工具。
 * 工具的 {@code inputSchema} 根据方法参数（含 {@link McpToolParam}）自动生成。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {

	/**
	 * 工具名称（必填，需在同一个 Server 实例内唯一）
	 */
	String name();

	/**
	 * 工具描述
	 */
	String description() default "";

	/**
	 * 工具标题（2025-11-25 可选字段，用于人类可读展示）
	 */
	String title() default "";

	/**
	 * 图标（2025-11-25 可选字段，透传给客户端）
	 */
	String icon() default "";
}
