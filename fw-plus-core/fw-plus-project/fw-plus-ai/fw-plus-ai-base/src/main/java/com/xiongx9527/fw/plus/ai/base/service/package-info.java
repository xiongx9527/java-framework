/**
 * AI 服务：把「一个接口 + 注解」变成可调用的模型能力。
 *
 * <p>调用方只声明接口方法与提示词模板，由 {@link com.xiongx9527.fw.plus.ai.base.service.AiServices}
 * 生成代理：解析模板变量 → 拼接记忆消息 → 调 {@code LlmPort} → 返回结果。
 * 这样业务代码不需要直接拼 {@code LlmRequest}，也不必关心流式 / 非流式。</p>
 */
package com.xiongx9527.fw.plus.ai.base.service;
