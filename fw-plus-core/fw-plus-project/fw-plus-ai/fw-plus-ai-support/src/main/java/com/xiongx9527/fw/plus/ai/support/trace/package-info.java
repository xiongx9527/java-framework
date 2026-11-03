/**
 * 链路追踪（监测）的组合层：把 base 各能力的事件监听器转成 span，发给 Langfuse / LangSmith。
 *
 * <p>base 只定义中性 span 与 exporter 端口；本包负责「接线」——监听 LLM / 检索调用，
 * 由 {@link TracePort} 背后的 exporter 翻译成具体后端报文。</p>
 */
package com.xiongx9527.fw.plus.ai.support.trace;
