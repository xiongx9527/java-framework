package com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.opentelemetry.OpentelemetryTrace;
import com.xiongx9527.fw.plus.common.trace.AbstractTrace;
import com.plumelog.core.LogMessageThreadLocal;
import com.plumelog.core.TraceId;
import com.plumelog.core.TraceMessage;
import com.plumelog.core.constant.LogMessageConstant;
import com.plumelog.core.util.GfJsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Function;

@Slf4j
public class PlumelogTrace extends AbstractTrace {

    @Autowired(required = false)
    private OpentelemetryTrace opentelemetryTrace;

    public boolean startTraceLog(String messageType) {
        return startTraceLog(log, messageType);
    }

    public boolean startTraceLog(org.slf4j.Logger log, String messageType) {
        try {
            String traceId = TraceId.logTraceID.get();
            if (StringUtils.isNotBlank(traceId)) {
                TraceMessage traceMessage = LogMessageThreadLocal.logMessageThreadLocal.get();
                if (traceMessage == null) {
                    traceMessage = new TraceMessage();
                    traceMessage.getPositionNum().set(0);
                }
                traceMessage.setTraceId(traceId);
                traceMessage.setMessageType(messageType);
                traceMessage.setPosition(LogMessageConstant.TRACE_START);
                traceMessage.getPositionNum().incrementAndGet();
                LogMessageThreadLocal.logMessageThreadLocal.set(traceMessage);
                log.info(LogMessageConstant.TRACE_PRE + GfJsonUtil.toJSONString(traceMessage));
                return true;
            }
        } catch (Exception e) {
            log.error("startTraceLog error", e);
        }
        return false;
    }

    public boolean endTraceLog(String messageType) {
        return endTraceLog(log, messageType);
    }

    public boolean endTraceLog(org.slf4j.Logger log, String messageType) {
        try {
            String traceId = TraceId.logTraceID.get();
            if (StringUtils.isNotBlank(traceId)) {
                TraceMessage traceMessage = LogMessageThreadLocal.logMessageThreadLocal.get();
                if (traceMessage != null) {
                    traceMessage.setMessageType(messageType);
                    traceMessage.setPosition(LogMessageConstant.TRACE_END);
                    traceMessage.getPositionNum().incrementAndGet();
                    log.info(LogMessageConstant.TRACE_PRE + GfJsonUtil.toJSONString(traceMessage));
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("endTraceLog error", e);
        }
        return false;
    }

    public Object traceLog(String messageType, Function<?, ?> function) {
        return traceLog(log, messageType, function);
    }

    public Object traceLog(org.slf4j.Logger log, String messageType, Function<?, ?> function) {
        boolean start = startTraceLog(log, messageType);
        try {
            return function.apply(null);
        } finally {
            if (start) {
                endTraceLog(log, messageType);
            }
        }
    }

    @Override
    public String getTraceId() {
        String traceId = null;
        if (opentelemetryTrace != null) {
            try {
                traceId = opentelemetryTrace.getTraceId();
            } catch (Exception e) {
                log.trace("Failed to get traceId from OpenTelemetry", e);
            }
        }

        if (StringUtils.isBlank(traceId)) {
            traceId = TraceId.logTraceID.get();
        } else {
            TraceId.logTraceID.set(traceId);
        }

        return traceId;
    }

    @Override
    public String getSpanId() {
        if (opentelemetryTrace != null) {
            try {
                return opentelemetryTrace.getSpanId();
            } catch (Exception e) {
                log.trace("Failed to get spanId from OpenTelemetry", e);
            }
        }
        return "";
    }

    @Override
    public void setAttribute(String key, String value) {
        if (opentelemetryTrace != null) {
            opentelemetryTrace.setAttribute(key, value);
        }
    }

    @Override
    public void setAttribute(String key, long value) {
        if (opentelemetryTrace != null) {
            opentelemetryTrace.setAttribute(key, value);
        }
    }

    @Override
    public void setAttribute(String key, boolean value) {
        if (opentelemetryTrace != null) {
            opentelemetryTrace.setAttribute(key, value);
        }
    }

    @Override
    public void setAttribute(String key, double value) {
        if (opentelemetryTrace != null) {
            opentelemetryTrace.setAttribute(key, value);
        }
    }

    @Override
    public void addEvent(String name) {
        if (opentelemetryTrace != null) {
            opentelemetryTrace.addEvent(name);
        }
    }

    @Override
    public void recordException(Throwable throwable) {
        if (opentelemetryTrace != null) {
            opentelemetryTrace.recordException(throwable);
        }
    }

}