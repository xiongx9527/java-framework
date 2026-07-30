package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model;

import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ModelDataSourceInfo extends MagicEntity {

    private String name;
    private String key;
    private String framework;
    private String provider;
    private AiModelType modelType;
    private boolean enabled = true;
    private String baseUrl;
    private String apiKey;
    private List<String> modelName = new ArrayList<String>();
    private String defaultModelName;
    private Map<String, ModelConfig> modelConfigs = new LinkedHashMap<String, ModelConfig>();
    private String secretKey;
    private String accessToken;
    private String token;
    private String password;
    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    public AiModelDefinition toDefinition() {
        List<String> modelNames = modelNames();
        String singleModelName = modelNames.isEmpty() ? null : modelNames.get(0);
        return toDefinition(key, singleModelName, modelConfig(singleModelName));
    }

    public AiModelDefinition toDefinition(String definitionKey, String resolvedModelName, ModelConfig modelConfig) {
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey(definitionKey);
        definition.setFramework(modelConfig != null && !isBlank(modelConfig.getFramework())
                ? modelConfig.getFramework() : framework);
        definition.setProvider(modelConfig != null && !isBlank(modelConfig.getProvider()) ? modelConfig.getProvider() : provider);
        definition.setType(modelConfig != null && modelConfig.getModelType() != null ? modelConfig.getModelType() : modelType);
        definition.setEnabled(enabled);
        Map<String, Object> definitionProperties = new LinkedHashMap<String, Object>();
        if (properties != null) {
            definitionProperties.putAll(properties);
        }
        if (modelConfig != null && modelConfig.getProperties() != null) {
            definitionProperties.putAll(modelConfig.getProperties());
        }
        putCredential(definitionProperties, "baseUrl",
                modelConfig != null && !isBlank(modelConfig.getBaseUrl()) ? modelConfig.getBaseUrl() : baseUrl);
        putCredential(definitionProperties, "apiKey",
                modelConfig != null && !isBlank(modelConfig.getApiKey()) ? modelConfig.getApiKey() : apiKey);
        putCredential(definitionProperties, "modelName", resolvedModelName);
        putCredential(definitionProperties, "secretKey", secretKey);
        putCredential(definitionProperties, "accessToken", accessToken);
        putCredential(definitionProperties, "token", token);
        putCredential(definitionProperties, "password", password);
        definition.setProperties(definitionProperties);
        return definition;
    }

    public List<String> modelNames() {
        List<String> names = new ArrayList<String>();
        if (modelName == null) {
            return names;
        }
        for (String name : modelName) {
            names.add(name == null ? null : name.trim());
        }
        return names;
    }

    public ModelConfig modelConfig(String name) {
        if (isBlank(name) || modelConfigs == null) {
            return null;
        }
        return modelConfigs.get(name);
    }

    @Override
    public ModelDataSourceInfo simple() {
        ModelDataSourceInfo info = copyBase(false);
        info.setProperties(withoutSensitiveProperties(properties));
        return info;
    }

    @Override
    public ModelDataSourceInfo copy() {
        ModelDataSourceInfo info = copyBase(true);
        info.setProperties(properties == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(properties));
        return info;
    }

    private ModelDataSourceInfo copyBase(boolean includeSensitiveFields) {
        ModelDataSourceInfo info = new ModelDataSourceInfo();
        if (includeSensitiveFields) {
            super.copyTo(info);
        } else {
            super.simple(info);
        }
        info.setName(name);
        info.setKey(key);
        info.setFramework(framework);
        info.setProvider(provider);
        info.setModelType(modelType);
        info.setEnabled(enabled);
        info.setBaseUrl(baseUrl);
        info.setModelName(modelName == null ? new ArrayList<String>() : new ArrayList<String>(modelName));
        info.setDefaultModelName(defaultModelName);
        info.setModelConfigs(copyModelConfigs(modelConfigs, includeSensitiveFields));
        if (includeSensitiveFields) {
            info.setApiKey(apiKey);
            info.setSecretKey(secretKey);
            info.setAccessToken(accessToken);
            info.setToken(token);
            info.setPassword(password);
        }
        return info;
    }

    private Map<String, ModelConfig> copyModelConfigs(Map<String, ModelConfig> source, boolean includeSensitiveFields) {
        Map<String, ModelConfig> result = new LinkedHashMap<String, ModelConfig>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, ModelConfig> entry : source.entrySet()) {
            result.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().copy(includeSensitiveFields));
        }
        return result;
    }

    private static Map<String, Object> withoutSensitiveProperties(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (!isSensitiveKey(entry.getKey())) {
                result.put(entry.getKey(), sanitizeValue(entry.getValue()));
            }
        }
        return result;
    }

    private static Object sanitizeValue(Object value) {
        if (value instanceof Map) {
            Map<Object, Object> result = new LinkedHashMap<Object, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof String) || !isSensitiveKey((String) key)) {
                    result.put(key, sanitizeValue(entry.getValue()));
                }
            }
            return result;
        }
        if (value instanceof Collection) {
            List<Object> result = new ArrayList<Object>();
            for (Object item : (Collection<?>) value) {
                result.add(sanitizeValue(item));
            }
            return result;
        }
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            Object result = Array.newInstance(value.getClass().getComponentType(), length);
            for (int index = 0; index < length; index++) {
                Array.set(result, index, sanitizeValue(Array.get(value, index)));
            }
            return result;
        }
        return value;
    }

    private static boolean isSensitiveKey(String propertyKey) {
        return "apiKey".equalsIgnoreCase(propertyKey)
                || "secretKey".equalsIgnoreCase(propertyKey)
                || "accessToken".equalsIgnoreCase(propertyKey)
                || "token".equalsIgnoreCase(propertyKey)
                || "password".equalsIgnoreCase(propertyKey);
    }

    private void putCredential(Map<String, Object> target, String propertyKey, String value) {
        if (value != null) {
            target.put(propertyKey, value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    @Data
    public static class ModelConfig {

        private Boolean enabled;
        private String framework;
        private String provider;
        private AiModelType modelType;
        private String baseUrl;
        private String apiKey;
        private Map<String, Object> properties = new LinkedHashMap<String, Object>();

        private ModelConfig copy(boolean includeSensitiveFields) {
            ModelConfig config = new ModelConfig();
            config.setEnabled(enabled);
            config.setFramework(framework);
            config.setProvider(provider);
            config.setModelType(modelType);
            config.setBaseUrl(baseUrl);
            if (includeSensitiveFields) {
                config.setApiKey(apiKey);
                config.setProperties(properties == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(properties));
            } else {
                config.setProperties(withoutSensitiveProperties(properties));
            }
            return config;
        }
    }
}
