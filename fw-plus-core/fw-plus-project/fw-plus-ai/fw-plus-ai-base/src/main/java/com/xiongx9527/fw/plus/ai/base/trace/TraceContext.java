package com.xiongx9527.fw.plus.ai.base.trace;

/**
 * 链路上下文：线程本地保存「当前 traceId」与「当前 spanId」，用于父子串联。
 *
 * <p>跨线程（流式 IO / 异步检索）时由调用方手动 {@link #fork(String, String)} 到新线程。</p>
 */
public final class TraceContext {

    private static final ThreadLocal<Entry> HOLDER = new ThreadLocal<Entry>();

    private TraceContext() {
    }

    /** 绑定当前线程的 trace / span。 */
    public static void bind(String traceId, String spanId) {
        HOLDER.set(new Entry(traceId, spanId));
    }

    /** 当前 traceId；未绑定为 {@code null}。 */
    public static String traceId() {
        Entry entry = HOLDER.get();
        return entry == null ? null : entry.traceId;
    }

    /** 当前 spanId（作为子节点的父）。 */
    public static String spanId() {
        Entry entry = HOLDER.get();
        return entry == null ? null : entry.spanId;
    }

    /** 解绑当前线程。 */
    public static void unbind() {
        HOLDER.remove();
    }

    /** 在子线程/回调里重建上下文。 */
    public static void fork(String traceId, String spanId) {
        bind(traceId, spanId);
    }

    private static final class Entry {

        private final String traceId;

        private final String spanId;

        private Entry(String traceId, String spanId) {
            this.traceId = traceId;
            this.spanId = spanId;
        }
    }
}
