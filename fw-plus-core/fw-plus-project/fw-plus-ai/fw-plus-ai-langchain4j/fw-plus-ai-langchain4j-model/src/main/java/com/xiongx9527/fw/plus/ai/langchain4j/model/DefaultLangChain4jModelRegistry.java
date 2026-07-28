package com.xiongx9527.fw.plus.ai.langchain4j.model;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.model.DefaultAiModelRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DefaultLangChain4jModelRegistry implements LangChain4jModelRegistry {

    private final Map<String, LangChain4jModel> models;
    private final AiModelRegistry neutralRegistry;

    public DefaultLangChain4jModelRegistry(Map<String, LangChain4jModel> models, String defaultModelKey) {
        Map<String, LangChain4jModel> modelCopy = new LinkedHashMap<String, LangChain4jModel>();
        if (models != null) {
            modelCopy.putAll(models);
        }
        Map<String, AiModel> neutralModels = new LinkedHashMap<String, AiModel>();
        for (Map.Entry<String, LangChain4jModel> entry : modelCopy.entrySet()) {
            neutralModels.put(entry.getKey(), neutral(entry.getKey(), entry.getValue()));
        }
        this.models = Collections.unmodifiableMap(modelCopy);
        try {
            this.neutralRegistry = new DefaultAiModelRegistry(neutralModels, defaultModelKey);
        } catch (AiModelException e) {
            throw new LangChain4jModelException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public LangChain4jModel get(String key) {
        try {
            neutralRegistry.get(key);
            return models.get(key);
        } catch (AiModelException e) {
            throw new LangChain4jModelException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public LangChain4jModel getDefault() {
        try {
            AiModel model = neutralRegistry.getDefault();
            return models.get(model.getKey());
        } catch (AiModelException e) {
            throw new LangChain4jModelException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public String getDefaultKey() {
        return neutralRegistry.getDefaultKey();
    }

    @Override
    public boolean contains(String key) {
        return neutralRegistry.contains(key);
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(neutralRegistry.keys()));
    }

    private AiModel neutral(String key, LangChain4jModel model) {
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey(key);
        definition.setFramework("langchain4j");
        definition.setProvider(model == null ? null : model.getProvider());
        definition.setType(model == null || model.getModelType() == null ? null
                : AiModelType.valueOf(model.getModelType().name()));
        definition.setEnabled(true);
        return new LangChain4jAiModel(definition, model);
    }
}
