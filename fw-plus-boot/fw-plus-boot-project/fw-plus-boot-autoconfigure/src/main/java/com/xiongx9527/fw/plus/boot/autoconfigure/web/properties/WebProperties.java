package com.xiongx9527.fw.plus.boot.autoconfigure.web.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;


/**
 * Web自动配置属性类
 */
@ConfigurationProperties(prefix = "fw.plus.web")
@Data
public class WebProperties {

    /**
     * 慢请求监控配置
     */
    private SlowRequest slowRequest = new SlowRequest();

    /**
     * 慢请求监控配置内部类
     */
    @Data
    public static class SlowRequest {
        /**
         * 是否启用慢请求监控
         */
        private boolean enabled = true;

        /**
         * 是否打印异常详细日志
         */
        private boolean printExceptionDetails = true;

        /**
         * 慢请求阈值配置
         * key: 阈值时间(毫秒)
         * value: 告警级别(WARN/ERROR/CRITICAL)
         */
        private Map<Long, String> thresholdLevels = new HashMap<Long, String>() {{
            put(1000L, "WARN");
            put(3000L, "ERROR");
            put(10000L, "CRITICAL");
        }};
    }

    // 获取慢请求监控开关的getter方法
    public boolean isSlowRequestEnabled() {
        return slowRequest.isEnabled();
    }
}
