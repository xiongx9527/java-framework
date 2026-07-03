package com.xiongx9527.fw.plus.common.trace;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.commons.lang3.StringUtils;

public class SpanIdConverter extends ClassicConverter {

    /**
     * 转换日志事件为对应的跨度ID
     * 该方法从当前线程的追踪上下文中获取跨度ID并返回
     *
     * @param event 日志事件对象，用于日志记录的上下文信息
     * @return 当前追踪上下文中的跨度ID字符串，如果不存在则返回null或空字符串
     */
    @Override
    public String convert(ILoggingEvent event) {
        String spanId = AbstractTrace.get().getSpanId();
        return StringUtils.isNotBlank(spanId) ? spanId : "no-span-id";
    }

}
