package com.xiongx9527.fw.plus.ai.base.trace;

/**
 * 链路导出器：把完成的 {@link Span} 发到观测后端。
 *
 * <p>Langfuse / LangSmith / OTLP 都是这个端口的实现，由 adapter 提供具体 HTTP / SDK 调用；
 * base 只定义中性结构，不引入任何观测 SDK 依赖。</p>
 */
public interface TraceExporter {

    /** 导出一条已结束的节点；实现必须吞掉自身异常，不得影响主链路。 */
    void export(Span span);
}
