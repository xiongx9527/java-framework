/**
 * 工具调用契约：工具声明（{@code @Tool} / {@code @P}）、规格（{@link com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification}）、
 * 调用请求 / 结果，以及反射式默认执行器。
 *
 * <p>base 只描述「工具有什么、怎么调」，不含调用环（模型 → tool_calls → 执行 → 回填）——
 * 那是 adapter 的 ToolCallingPort 职责。</p>
 */
package com.xiongx9527.fw.plus.ai.base.tool;
