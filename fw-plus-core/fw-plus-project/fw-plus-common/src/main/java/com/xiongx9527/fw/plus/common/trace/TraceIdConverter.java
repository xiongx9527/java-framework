package com.xiongx9527.fw.plus.common.trace;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.commons.lang3.StringUtils;

public class TraceIdConverter extends ClassicConverter {

    /**
     * 转换日志事件为跟踪ID
     * 该方法用于从当前线程的跟踪上下文中获取跟踪ID，用于日志追踪
     *
     * @param event 日志事件对象，包含日志的相关信息
     * @return 返回当前线程上下文中的跟踪ID字符串，如果不存在则返回null
     */
    @Override
    public String convert(ILoggingEvent event) {
        String traceId = AbstractTrace.get().getTraceId();
        return StringUtils.isNotBlank(traceId) ? traceId : "no-trace-id";
    }


}
