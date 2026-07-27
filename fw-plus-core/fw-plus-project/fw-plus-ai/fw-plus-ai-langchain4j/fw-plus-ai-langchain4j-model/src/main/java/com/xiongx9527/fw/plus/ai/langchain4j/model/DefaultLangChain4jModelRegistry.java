package com.xiongx9527.fw.plus.ai.langchain4j.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DefaultLangChain4jModelRegistry implements LangChain4jModelRegistry {

    private final Map<String, LangChain4jModel> models;

    private final String defaultModelKey;

    public DefaultLangChain4jModelRegistry(Map<String, LangChain4jModel> models, String defaultModelKey) {
        Map<String, LangChain4jModel> modelCopy = new LinkedHashMap<String, LangChain4jModel>();
        if (models != null) {
            modelCopy.putAll(models);
        }
        if (!isBlank(defaultModelKey) && !modelCopy.containsKey(defaultModelKey)) {
            throw new LangChain4jModelException("Default model key does not exist: " + defaultModelKey);
        }
        this.models = Collections.unmodifiableMap(modelCopy);
        this.defaultModelKey = defaultModelKey;
    }

    @Override
    public LangChain4jModel get(String key) {
        LangChain4jModel model = models.get(key);
        if (model == null) {
            throw new LangChain4jModelException("Model key does not exist: " + key);
        }
        return model;
    }

    @Override
    public LangChain4jModel getDefault() {
        if (isBlank(defaultModelKey)) {
            throw new LangChain4jModelException("Default model key is not configured");
        }
        return get(defaultModelKey);
    }

    @Override
    public String getDefaultKey() {
        return defaultModelKey;
    }

    @Override
    public boolean contains(String key) {
        return models.containsKey(key);
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(models.keySet()));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
