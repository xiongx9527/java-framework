/**
 * native 组件：直连基础能力。
 *
 * <p>HTTP / SSE 传输、JSON 编解码、重试与错误映射；不依赖 base / support，也不依赖任何 AI 框架。
 *
 * <p><b>出处</b>：C 类（自行抽象）——native 后端不走 langchain4j / Spring AI，直连厂商 API；
 * 端口形状以 {@code fw-plus-ai-base} 与 {@code fw-plus-ai-support} 的契约为准。
 *
 * @author xiongxiao
 */
package com.xiongx9527.fw.plus.ai.native.core;
