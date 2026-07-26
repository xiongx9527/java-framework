package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ModelDataSourceInfo extends MagicEntity {

    private String name;
    private String key;
    private String provider;
    private LangChain4jModelType modelType;
    private boolean enabled = true;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private String secretKey;
    private String accessToken;
    private String token;
    private String password;
    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    public LangChain4jModelDefinition toDefinition() {
        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey(key);
        definition.setProvider(provider);
        definition.setModelType(modelType);
        definition.setEnabled(enabled);
        definition.setBaseUrl(baseUrl);
        definition.setApiKey(apiKey);
        definition.setModelName(modelName);
        Map<String, Object> definitionProperties = new LinkedHashMap<String, Object>();
        if (properties != null) {
            definitionProperties.putAll(properties);
        }
        putCredential(definitionProperties, "apiKey", apiKey);
        putCredential(definitionProperties, "secretKey", secretKey);
        putCredential(definitionProperties, "accessToken", accessToken);
        putCredential(definitionProperties, "token", token);
        putCredential(definitionProperties, "password", password);
        definition.setProperties(definitionProperties);
        return definition;
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
        info.setProperties(new LinkedHashMap<String, Object>(properties));
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
        info.setProvider(provider);
        info.setModelType(modelType);
        info.setEnabled(enabled);
        info.setBaseUrl(baseUrl);
        info.setModelName(modelName);
        if (includeSensitiveFields) {
            info.setApiKey(apiKey);
            info.setSecretKey(secretKey);
            info.setAccessToken(accessToken);
            info.setToken(token);
            info.setPassword(password);
        }
        return info;
    }

    private Map<String, Object> withoutSensitiveProperties(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (!isSensitiveKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private boolean isSensitiveKey(String propertyKey) {
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
}
