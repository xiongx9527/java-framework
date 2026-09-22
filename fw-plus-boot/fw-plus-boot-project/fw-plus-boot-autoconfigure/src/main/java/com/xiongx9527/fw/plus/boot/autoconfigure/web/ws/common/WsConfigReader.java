package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import java.util.Map;
import java.util.function.Function;

/**
 * 扁平配置读取工具。
 *
 * <p>WebSocket 地基内部统一按点号扁平键读取配置，这里集中处理取值与类型转换，
 * 转换失败或缺失一律回落默认值，避免配置写错导致启动失败。</p>
 *
 * @author zzy
 */
public final class WsConfigReader {

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
            // 忽略转换异常，使用默认值
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
}
