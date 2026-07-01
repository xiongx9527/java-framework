package com.xx9527.fw.plus.common.trace.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.xx9527.fw.plus.common.trace.AbstractTrace;

public class SpanIdConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return AbstractTrace.currentSpanId();
    }
}
