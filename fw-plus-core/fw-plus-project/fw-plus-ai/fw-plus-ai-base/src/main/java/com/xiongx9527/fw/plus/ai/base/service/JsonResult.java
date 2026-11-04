package com.xiongx9527.fw.plus.ai.base.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记某个 AI 服务方法「期望模型稳定输出 JSON」。
 *
 * <p>被标注的方法在 {@link AiServices} 中会自动走 {@code JsonOutputPort}：</p>
 * <ul>
 *   <li>非流式：强制 + 兜底提取（注入 JSON 指令、必要时下发后端 JSON 模式、解析失败重试与修复），
 *        返回值即提取出的 JSON 文本；</li>
 *   <li>流式：注入 JSON 指令并增量转发 JSON 主体（剥离前导散文 / markdown 围栏），闭合即停止，
 *        结果通过传入的 {@code LlmStreamHandler} 透传。</li>
 * </ul>
 *
 * <p><b>返回类型约定：</b></p>
 * <ul>
 *   <li>返回 {@code String}：直接返回提取出的 JSON 文本；</li>
 *   <li>返回 {@link com.xiongx9527.fw.plus.ai.base.model.LlmResponse}：返回包裹该 JSON 文本的响应（含 usage）；</li>
 *   <li>返回其它 POJO（含 {@code List<Foo>} 等参数化类型）：需通过
 *       {@link AiServices#builder(Class)} 的 {@code .jsonBindings(JsonBindings)} 接入一个绑定器
 *       （如 support 提供的 {@code JacksonJsonBindings}），框架会把 JSON 文本反序列化为该类型；
 *       若未接入绑定器则抛 {@link IllegalStateException}（避免把 JSON 文本强转为 POJO 引发 ClassCastException）。</li>
 * </ul>
 *
 * <p>对「无工具」与「带工具」的方法均生效：带工具时会在工具循环结束后从最终回答中稳定提取 JSON；
 * 若方法以流式返回，则增量透传 JSON 主体（剥离前导散文 / markdown 围栏）。</p>
 *
 * @see com.xiongx9527.fw.plus.ai.base.output.JsonBindings
 * @see com.xiongx9527.fw.plus.ai.base.output.JsonOutputPort
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JsonResult {
}
