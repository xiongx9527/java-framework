package com.xiongx9527.fw.plus.ai.base.trace;

/**
 * {@link TracePort} 的空实现：开启的节点直接丢弃。
 */
public final class NoopTracePort implements TracePort {

    static final NoopTracePort INSTANCE = new NoopTracePort();

    @Override
    public Span.Builder start(String name, SpanKind kind) {
        return Span.start(null, name, kind);
    }

    @Override
    public void end(Span.Builder span) {
        // 丢弃
    }

    @Override
    public void flush() {
        // 无缓冲
    }
}
