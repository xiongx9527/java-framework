/**
 * 工具调用契约：工具声明（{@code @Tool} / {@code @P}）、规格
 * （{@link com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification}）、调用请求 / 结果，
 * 以及反射式默认执行器与调用环。
 *
 * <p>base 内既描述「工具有什么、怎么调」，也提供
 * {@link com.xiongx9527.fw.plus.ai.base.tool.ToolCallingLoop} 把
 * 「模型 → tool_calls → 执行 → 回填」的多轮循环收敛成一个入口；
 * 具体工具来源由业务侧通过 {@link com.xiongx9527.fw.plus.ai.base.tool.ToolProvider} 注入。</p>
 */
package com.xiongx9527.fw.plus.ai.base.tool;
