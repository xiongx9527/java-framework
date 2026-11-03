/**
 * 链路追踪（监测）：与 Langfuse / LangSmith / OTLP 解耦的中性结构。
 *
 * <p>base 只定义 span 快照、exporter 端口与线程本地上下文；具体后端 exporter（HTTP / SDK）
 * 由 adapter 实现，业务通过 {@code support.trace} 的监听器把各能力事件转成 span。</p>
 */
package com.xiongx9527.fw.plus.ai.base.trace;
