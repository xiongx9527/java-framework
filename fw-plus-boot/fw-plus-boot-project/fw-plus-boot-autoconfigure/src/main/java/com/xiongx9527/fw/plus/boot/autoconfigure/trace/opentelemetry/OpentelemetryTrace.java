package com.xiongx9527.fw.plus.boot.autoconfigure.trace.opentelemetry;

import com.xiongx9527.fw.plus.common.trace.AbstractTrace;

public class OpentelemetryTrace extends AbstractTrace {

    /**
     * 无效的追踪ID常量
     * 用于表示无效或未初始化的分布式追踪ID
     */
    private static final String INVALID_TRACE_ID = "00000000000000000000000000000000";

    /**
     * 无效的跨度ID常量
     * 用于表示无效或未初始化的分布式追踪跨度ID
     */
    private static final String INVALID_SPAN_ID = "0000000000000000";

    /**
     * 获取当前追踪的Trace ID
     *
     * @return 当前追踪的Trace ID，如果获取失败则返回"no-trace-id"
     */
    private String traceId() {
        String traceId = null;
        try {
            traceId = io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId();
        } catch (Throwable e) {
        }
        if (INVALID_TRACE_ID.equals(traceId)) {
            traceId = null;
        }
        return traceId;
    }

    /**
     * 获取当前跨度的Span ID
     *
     * @return 当前跨度的Span ID，如果获取失败则返回"no-span-id"
     */
    private String spanId() {
        String spanId = null;
        try {
            spanId = io.opentelemetry.api.trace.Span.current().getSpanContext().getSpanId();
        } catch (Throwable e) {
        }
        if (INVALID_SPAN_ID.equals(spanId)) {
            spanId = null;
        }
        return spanId;
    }

    /**
     * 获取追踪ID的实现方法
     *
     * @return 追踪ID字符串
     */
    @Override
    public String getTraceId() {
        return traceId();
    }

    /**
     * 获取跨度ID的实现方法
     *
     * @return 跨度ID字符串
     */
    @Override
    public String getSpanId() {
        return spanId();
    }



}
