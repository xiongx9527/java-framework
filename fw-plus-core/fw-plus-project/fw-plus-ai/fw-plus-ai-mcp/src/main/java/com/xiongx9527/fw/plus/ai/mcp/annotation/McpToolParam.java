package com.xiongx9527.fw.plus.ai.mcp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP 工具参数元数据注解
 *
 * <p>对齐 spec 7.2：与 {@link McpTool} 配合使用，标注在方法参数上，
 * 提供参数名、描述、是否必填等元数据，用于自动生成 inputSchema。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpToolParam {

	/**
	 * 参数名（覆盖编译期参数名）
	 */
	String name() default "";

	/**
	 * 参数描述
	 */
	String description() default "";

	/**
	 * 是否必填
	 */
	boolean required() default false;
}
