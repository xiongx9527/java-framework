package com.xiongx9527.fw.plus.boot.autoconfigure.trace.opentelemetry;

import com.xiongx9527.fw.plus.common.trace.AbstractTrace;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class OpentelemetryTrace extends AbstractTrace {

    private static final String INVALID_TRACE_ID = "00000000000000000000000000000000";
    private static final String INVALID_SPAN_ID = "0000000000000000";

    private String getSpanContextValue(Function<Span, String> extractor, String invalidValue) {
        try {
            String value = extractor.apply(Span.current());
            if (invalidValue.equals(value)) {
                return null;
            }
            return value;
        } catch (Throwable e) {
            log.trace("Failed to get span context value from OpenTelemetry", e);
            return null;
        }
    }

    @Override
    public String getTraceId() {
        return getSpanContextValue(
                span -> span.getSpanContext().getTraceId(),
                INVALID_TRACE_ID);
    }

    @Override
    public String getSpanId() {
        return getSpanContextValue(
                span -> span.getSpanContext().getSpanId(),
                INVALID_SPAN_ID);
    }

    @Override
    public void setAttribute(String key, String value) {
        if (value != null) {
            try {
                Span.current().setAttribute(key, value);
            } catch (Throwable e) {
                log.trace("Failed to set span attribute: {}={}", key, value, e);
            }
        }
    }

    @Override
    public void setAttribute(String key, long value) {
        try {
            Span.current().setAttribute(key, value);
        } catch (Throwable e) {
            log.trace("Failed to set span attribute: {}={}", key, value, e);
        }
    }

    @Override
    public void setAttribute(String key, boolean value) {
        try {
            Span.current().setAttribute(key, value);
        } catch (Throwable e) {
            log.trace("Failed to set span attribute: {}={}", key, value, e);
        }
    }

    @Override
    public void setAttribute(String key, double value) {
        try {
            Span.current().setAttribute(key, value);
        } catch (Throwable e) {
            log.trace("Failed to set span attribute: {}={}", key, value, e);
        }
    }

    @Override
    public void addEvent(String name) {
        try {
            Span.current().addEvent(name);
        } catch (Throwable e) {
            log.trace("Failed to add span event: {}", name, e);
        }
    }

    @Override
    public void recordException(Throwable throwable) {
        if (throwable != null) {
            try {
                Span.current().recordException(throwable);
                Span.current().setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, throwable.getMessage());
            } catch (Throwable e) {
                log.trace("Failed to record exception on span", e);
            }
        }
    }

}