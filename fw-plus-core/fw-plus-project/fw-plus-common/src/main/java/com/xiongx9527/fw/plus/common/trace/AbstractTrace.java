package com.xiongx9527.fw.plus.common.trace;

import com.xiongx9527.fw.plus.common.util.SpringUtils;

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

    /**
     * 获取追踪实例
     * 优先从Spring容器中获取实现类，如果获取失败则返回默认实现
     * @return 追踪实例对象
     */
    public static AbstractTrace get() {
        AbstractTrace trace = null;
        try {
            trace = SpringUtils.getBean(AbstractTrace.class);
        } catch (Exception e) {
            // ignore
            trace = new DefaultTrace();
        }
        return trace;
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
