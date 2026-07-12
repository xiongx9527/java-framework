package com.xiongx9527.fw.plus.common.trace.log4j2;

import com.xiongx9527.fw.plus.common.trace.AbstractTrace;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

@Plugin(name = "traceId", category = "Converter")
@ConverterKeys({"traceId"})
public class TraceIdConverter extends LogEventPatternConverter {

    public static TraceIdConverter newInstance(String[] options) {
        return new TraceIdConverter("traceId", null);
    }

    protected TraceIdConverter(String name, String style) {
        super(name, style);
    }

    @Override
    public void format(LogEvent event, StringBuilder output) {
        output.append(AbstractTrace.currentTraceId());
    }
}
