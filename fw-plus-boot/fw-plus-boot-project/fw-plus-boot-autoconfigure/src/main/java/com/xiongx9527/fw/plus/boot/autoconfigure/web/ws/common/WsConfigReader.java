package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;

/**
 * 扁平配置读取工具。
 *
 * <p>WebSocket 地基内部统一按点号扁平键读取配置，这里集中处理取值与类型转换。
 * 转换失败时回落默认值并告警，既不因为一处配置写错导致启动失败，也不让错误被静默吞掉。</p>
 *
 * @author zzy
 */
public final class WsConfigReader {

    private static final Logger log = LoggerFactory.getLogger(WsConfigReader.class);

    private WsConfigReader() {
    }

    /**
     * 按扁平键读取并转换配置值。
     *
     * @param configMap    点号扁平键配置映射，允许为 null
     * @param configKey    配置键
     * @param defaultValue 默认值
     * @param converter    字符串转换器
     * @param <T>          目标类型
     * @return 转换后的配置值，缺失或转换失败时返回默认值
     */
    public static <T> T get(Map<String, Object> configMap, String configKey, T defaultValue,
                            Function<String, T> converter) {
        if (configMap == null) {
            return defaultValue;
        }
        try {
            Object valueObj = configMap.get(configKey);
            if (valueObj != null) {
                return converter.apply(valueObj.toString());
            }
        } catch (Exception e) {
            log.warn("Invalid value for fw.plus.ws.{}: {} - falling back to default: {}",
                    configKey, mask(configMap.get(configKey)), defaultValue);
        }
        return defaultValue;
    }

    /**
     * 读取长整型配置值。
     */
    public static long getAsLong(Map<String, Object> configMap, String configKey, long defaultValue) {
        return get(configMap, configKey, defaultValue, Long::parseLong);
    }

    /**
     * 读取整型配置值。
     */
    public static int getAsInt(Map<String, Object> configMap, String configKey, int defaultValue) {
        return get(configMap, configKey, defaultValue, Integer::parseInt);
    }

    /**
     * 读取字符串配置值，空串也回落默认值。
     */
    public static String getAsString(Map<String, Object> configMap, String configKey, String defaultValue) {
        return get(configMap, configKey, defaultValue, v -> (v != null && !v.isEmpty()) ? v : defaultValue);
    }

    /**
     * 读取布尔型配置值。
     */
    public static boolean getAsBoolean(Map<String, Object> configMap, String configKey, boolean defaultValue) {
        return get(configMap, configKey, defaultValue, Boolean::parseBoolean);
    }

    /**
     * 对配置值做脱敏，避免密钥等凭证被完整写入日志。
     *
     * @param value 原始配置值
     * @return 脱敏后的值
     */
    private static Object mask(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.isEmpty()) {
            return text;
        }
        if (text.length() <= 8) {
            return "***";
        }
        return text.substring(0, 4) + "****" + text.substring(text.length() - 4);
    }
}
