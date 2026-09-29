package com.xiongx9527.fw.plus.ai.mcp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将 {@code @McpService} 类或其方法归属到指定 key 的 MCP server runtime。
 *
 * <p>用法与 Quarkus MCP Server 对齐：标注在类型或方法上，
 * 值为 {@code servers} 配置中声明的 runtime key。</p>
 *
 * <ul>
 *     <li>标注在类型上：该类型下所有 MCP 能力（tool/resource/prompt）归属该 runtime。</li>
 *     <li>标注在方法上：仅该方法归属对应 runtime，方法级覆盖类型级。</li>
 *     <li>未标注时归属默认 runtime（key 为 {@code default}）。</li>
 * </ul>
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface McpServer {

    /**
     * runtime key；留空表示默认 runtime。
     */
    String value() default "";
}