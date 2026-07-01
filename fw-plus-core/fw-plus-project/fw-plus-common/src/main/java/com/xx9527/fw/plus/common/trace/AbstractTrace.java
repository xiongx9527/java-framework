package com.xx9527.fw.plus.common.trace;

import com.xx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractTrace {

    /**
     * 获取追踪ID
     * @return 追踪ID字符串
     */
    public abstract String getTraceId();

    /**
     * 获取跨度ID
     * @return 跨度ID字符串
     */
    public abstract String getSpanId();

    public void setAttribute(String key, String value) {
    }

    public void setAttribute(String key, long value) {
    }

    public void setAttribute(String key, boolean value) {
    }

    public void setAttribute(String key, double value) {
    }

    public void addEvent(String name) {
    }

    /**
     * 获取当前 traceId，为空时返回默认值。
     * 供各日志框架的 Converter 调用，避免重复判空逻辑。
     */
    public static String currentTraceId() {
        String traceId = get().getTraceId();
        return StringUtils.isNotBlank(traceId) ? traceId : "no-trace-id";
    }

    /**
     * 获取当前 spanId，为空时返回默认值。
     */
    public static String currentSpanId() {
        String spanId = get().getSpanId();
        return StringUtils.isNotBlank(spanId) ? spanId : "no-span-id";
    }

    public void recordException(Throwable throwable) {
    }

    /**
     * 获取追踪实例
     * 优先从Spring容器中获取实现类，如果获取失败则返回默认实现
     * @return 追踪实例对象
     */
    public static AbstractTrace get() {
        AbstractTrace trace;
        try {
            trace = SpringUtils.getBean(AbstractTrace.class);
        } catch (Exception e) {
            trace = null;
        }
        return trace != null ? trace : new DefaultTrace();
    }

    /**
     * 默认追踪实现类，继承自AbstractTrace抽象类
     * 用于在没有实际追踪信息时提供默认的追踪ID和跨度ID
     */
    static class DefaultTrace extends AbstractTrace {
        /**
         * 获取追踪ID
         * @return 返回默认的追踪ID字符串"no-trace-id"
         */
        @Override
        public String getTraceId() {
            return "no-trace-id";
        }

        /**
         * 获取跨度ID
         * @return 返回默认的跨度ID字符串"no-span-id"
         */
        @Override
        public String getSpanId() {
            return "no-span-id";
        }
    }


}