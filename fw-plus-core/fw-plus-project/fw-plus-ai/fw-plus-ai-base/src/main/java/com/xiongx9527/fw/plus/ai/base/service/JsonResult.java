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
 *       返回值即提取出的 JSON 文本；</li>
 *   <li>流式：注入 JSON 指令并增量转发 JSON 主体（剥离前导散文 / markdown 围栏），闭合即停止，
 *       结果通过传入的 {@code LlmStreamHandler} 透传。</li>
 * </ul>
 *
 * <p>对「无工具」与「带工具」的方法均生效：带工具时会在工具循环结束后从最终回答中稳定提取 JSON；
 * 若方法以流式返回，则增量透传 JSON 主体（剥离前导散文 / markdown 围栏）。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JsonResult {
}
