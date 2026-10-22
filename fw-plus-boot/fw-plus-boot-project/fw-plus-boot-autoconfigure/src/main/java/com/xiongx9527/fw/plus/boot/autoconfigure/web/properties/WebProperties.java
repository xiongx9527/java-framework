package com.xiongx9527.fw.plus.boot.autoconfigure.web.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Locale;
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
     * 国际化配置
     */
    private I18n i18n = new I18n();

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
        private Map<Long, String> thresholdLevels = new HashMap<>();
        {
            thresholdLevels.put(1000L, "WARN");
            thresholdLevels.put(3000L, "ERROR");
            thresholdLevels.put(10000L, "CRITICAL");
        }
    }

    // 获取慢请求监控开关的getter方法
    public boolean isSlowRequestEnabled() {
        return slowRequest.isEnabled();
    }

    /**
     * 国际化配置内部类。
     *
     * <p>资源文件按模块分散放置（各模块 {@code src/main/resources/i18n/messages*.properties}），
     * 加载时扫描所有 {@code i18n/} 目录并把发现的基名合并。</p>
     */
    @Data
    public static class I18n {

        /** 是否启用国际化 */
        private boolean enabled = true;

        /** 资源扫描目录（classpath 通配），默认各模块的 {@code i18n/} */
        private String[] locations = new String[]{"classpath*:i18n/"};

        /** 缺省语言，形如 {@code zh_CN} */
        private String defaultLocale = "zh_CN";

        /** 手动切换语言的 URL 参数名 */
        private String paramName = "lang";

        /** 资源缓存秒数；-1 表示永久缓存（生产建议默认） */
        private int cacheSeconds = 3600;

        /**
         * 解析扫描目录：保证目录以 {@code /} 结尾且带 classpath 通配前缀。
         */
        public String[] resolveLocations() {
            if (locations == null || locations.length == 0) {
                return new String[]{"classpath*:i18n/"};
            }
            String[] result = new String[locations.length];
            for (int i = 0; i < locations.length; i++) {
                String location = locations[i];
                if (location == null || location.trim().isEmpty()) {
                    result[i] = "classpath*:i18n/";
                    continue;
                }
                location = location.trim();
                if (location.startsWith("classpath:")) {
                    location = "classpath*:" + location.substring("classpath:".length());
                } else if (!location.startsWith("classpath*:")) {
                    location = "classpath*:" + location;
                }
                if (!location.endsWith("/")) {
                    location = location + "/";
                }
                result[i] = location;
            }
            return result;
        }

        /**
         * 解析缺省语言；非法格式时回退中文。
         */
        public Locale resolveDefaultLocale() {
            String value = defaultLocale;
            if (value == null || value.trim().isEmpty()) {
                return Locale.SIMPLIFIED_CHINESE;
            }
            String[] parts = value.trim().split("[_-]");
            if (parts.length >= 2) {
                return new Locale(parts[0], parts[1]);
            }
            return new Locale(parts[0]);
        }
    }
}
