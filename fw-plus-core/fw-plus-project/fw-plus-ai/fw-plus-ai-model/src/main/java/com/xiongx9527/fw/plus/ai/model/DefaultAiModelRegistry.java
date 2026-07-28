package com.xiongx9527.fw.plus.ai.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultAiModelRegistry implements AiModelRegistry {

    private final Map<String, AiModel> models;

    private final String defaultModelKey;

    public DefaultAiModelRegistry(List<AiModelDefinition> definitions, String defaultModelKey,
                                  List<AiModelAdapter> adapters) {
        this(createModels(definitions, adapters), defaultModelKey);
    }

    public DefaultAiModelRegistry(Map<String, AiModel> models, String defaultModelKey) {
        Map<String, AiModel> modelCopy = new LinkedHashMap<String, AiModel>();
        if (models != null) {
            modelCopy.putAll(models);
        }
        if (!isBlank(defaultModelKey) && !modelCopy.containsKey(defaultModelKey)) {
            throw new AiModelException("Default model key does not exist: " + defaultModelKey);
        }
        this.models = Collections.unmodifiableMap(modelCopy);
        this.defaultModelKey = defaultModelKey;
    }

    @Override
    public AiModel get(String key) {
        AiModel model = models.get(key);
        if (model == null) {
            throw new AiModelException("Model key does not exist: " + key);
        }
        return model;
    }

    @Override
    public AiModel getDefault() {
        if (isBlank(defaultModelKey)) {
            throw new AiModelException("Default model key is not configured");
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

    private static Map<String, AiModel> createModels(List<AiModelDefinition> definitions,
                                                       List<AiModelAdapter> adapters) {
        Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
        AiModelFactory factory = new DefaultAiModelFactory();
        if (definitions == null) {
            return models;
        }
        for (AiModelDefinition definition : definitions) {
            if (definition == null || !definition.isEnabled()) {
                continue;
            }
            String key = definition.getKey();
            if (models.containsKey(key)) {
                throw new AiModelException("Duplicate enabled model key: " + key);
            }
            models.put(key, factory.create(definition, adapters));
        }
        return models;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
