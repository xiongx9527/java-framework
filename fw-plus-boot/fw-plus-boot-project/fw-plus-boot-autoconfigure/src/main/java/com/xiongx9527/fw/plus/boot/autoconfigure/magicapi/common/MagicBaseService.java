package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common;

import com.alibaba.fastjson2.JSONValidator;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.util.NetUtils;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.function.Function;

public interface MagicBaseService {


    /**
     * 生成全局唯一标识符
     * <p>
     * 该标识符由应用名称、服务端口号和MAC地址三部分组成，用于在分布式环境中唯一标识当前实例。
     * 格式为：{应用名称}-{端口号}-{MAC地址}
     * </p>
     *
     * @return String 唯一标识符字符串
     */
    default String getUniqueId() {
        String serviceName = SpringUtils.getApplicationName();
        String serverPort = SpringUtils.getServerPort();
        String macAddress = NetUtils.getMacAddress();
        return serviceName + "-" + serverPort + "-" + macAddress;
    }

    /**
     * 验证字段是否为空
     *
     * @param value 字段值
     * @param fieldName 字段名称
     * @throws IllegalArgumentException 当字段值为空时抛出此异常
     */
    default void validateRequiredField(Object value, String fieldName) {
        if (ObjectUtils.isEmpty(value)) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.magicapi.fieldRequired", fieldName));
        }
    }

    /**
     * 验证端口号是否有效
     *
     * @param port 端口号
     * @throws IllegalArgumentException 当端口号不在有效范围(1-65535)内时抛出此异常
     */
    default void validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.magicapi.portInvalid"));
        }
    }

    /**
     * 验证指定字段的JSON字符串是否有效
     * 
     * @param value 需要验证的JSON字符串
     * @param fieldName 字段名称，用于错误提示
     * @throws IllegalArgumentException 当JSON字符串无效或不是JSON对象格式时抛出
     */
    default void validateJSON(String value, String fieldName) {
        if (StringUtils.isNotBlank(value)) {
            JSONValidator jsonValidator = JSONValidator.from(value);
            // 验证JSON字符串有效性且必须为对象类型
            if (!jsonValidator.validate() || jsonValidator.getType() != JSONValidator.Type.Object) {
                throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.magicapi.fieldNotJson", fieldName));
            }
        }
    }

    default <T> T getConfigValue(Map<String, Object> configMap, String configKey, T defaultValue, Function<String, T> converter) {
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

    default long getConfigValueAsLong(Map<String, Object> configMap, String configKey, long defaultValue) {
        return getConfigValue(configMap, configKey, defaultValue, Long::parseLong);
    }

    default Float getConfigValueAsFloat(Map<String, Object> configMap, String configKey, float defaultValue) {
        return getConfigValue(configMap, configKey, defaultValue, Float::parseFloat);
    }

    default int getConfigValueAsInt(Map<String, Object> configMap, String configKey, int defaultValue) {
        return getConfigValue(configMap, configKey, defaultValue, Integer::parseInt);
    }

    default String getConfigValueAsString(Map<String, Object> configMap, String configKey, String defaultValue) {
        return getConfigValue(configMap, configKey, defaultValue, v -> (v != null && !v.isEmpty()) ? v : defaultValue);
    }

    default boolean getConfigValueAsBoolean(Map<String, Object> configMap, String configKey, boolean defaultValue) {
        return getConfigValue(configMap, configKey, defaultValue, Boolean::parseBoolean);
    }


}
