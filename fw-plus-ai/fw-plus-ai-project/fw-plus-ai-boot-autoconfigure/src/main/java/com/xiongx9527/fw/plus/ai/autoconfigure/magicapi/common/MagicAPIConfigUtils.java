package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.TypeReference;

import java.util.LinkedHashMap;
import java.util.Map;

public class MagicAPIConfigUtils extends com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils {

    private static final String AI_PREFIX = "magic.api.";

    public static String getVectorConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(AI_PREFIX + "vector.", key, configKey, defaultValue);
    }

    public static String getModelConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(AI_PREFIX + "model.", key, configKey, defaultValue);
    }

    public static Map<String, Object> mergeProperties(Map<String, Object> properties, String configuredProperties) {
        Map<String, Object> merged = new LinkedHashMap<String, Object>();
        if (configuredProperties == null || configuredProperties.trim().length() == 0) {
            if (properties != null) {
                merged.putAll(properties);
            }
            return merged;
        }
        try {
            Map<String, Object> configured = JSON.parseObject(configuredProperties,
                    new TypeReference<Map<String, Object>>() {
                    });
            if (configured != null) {
                merged.putAll(configured);
            }
        } catch (JSONException e) {
            if (properties != null) {
                merged.putAll(properties);
            }
        }
        return merged;
    }
}
