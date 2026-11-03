package com.xiongx9527.fw.plus.ai.base.trace;

import java.util.List;

/**
 * 链路追踪端口：开启节点、结束并导出。
 *
 * <p>节点按「开启 → 填充 → 结束」三步使用；父节点通过 {@link TraceContext} 串联，
 * 不用把 parentSpanId 在所有调用间手动传递。</p>
 */
public interface TracePort {

    /** 开启节点；返回可填充的 builder，记得最后 {@link #end(Span.Builder)}。 */
    Span.Builder start(String name, SpanKind kind);

    /** 结束并导出节点。 */
    void end(Span.Builder span);

    /** 批量刷新（有缓冲的导出器需要）。 */
    void flush();

    /** 什么都不做的实现，便于默认关闭观测。 */
    static TracePort noop() {
        return NoopTracePort.INSTANCE;
    }
}
