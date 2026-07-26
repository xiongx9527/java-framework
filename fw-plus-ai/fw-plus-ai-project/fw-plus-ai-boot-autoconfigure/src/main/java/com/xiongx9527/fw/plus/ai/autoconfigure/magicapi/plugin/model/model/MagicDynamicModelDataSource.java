package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelException;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MagicDynamicModelDataSource {

    private final LangChain4jModelRegistry springBootRegistry;
    private final LangChain4jModelFactory modelFactory;
    private final List<LangChain4jModelProviderAdapter> providerAdapters;
    private final Map<String, LangChain4jModel> runtimeModels = new ConcurrentHashMap<String, LangChain4jModel>();
    private final Map<String, ModelDataSourceInfo> runtimeInfos = new ConcurrentHashMap<String, ModelDataSourceInfo>();

    public MagicDynamicModelDataSource(LangChain4jModelRegistry springBootRegistry,
                                       LangChain4jModelFactory modelFactory,
                                       List<LangChain4jModelProviderAdapter> providerAdapters) {
        this.springBootRegistry = springBootRegistry;
        this.modelFactory = modelFactory;
        this.providerAdapters = providerAdapters == null ? Collections.<LangChain4jModelProviderAdapter>emptyList() : providerAdapters;
    }

    public void createClient(ModelDataSourceInfo info) {
        validate(info);
        ModelDataSourceInfo runtimeInfo = info.copy();
        LangChain4jModel model = modelFactory.create(info.toDefinition(), providerAdapters);
        runtimeModels.put(info.getKey(), model);
        runtimeInfos.put(info.getKey(), runtimeInfo);
    }

    public void destroyClientByKey(String key) {
        if (isBlank(key)) {
            return;
        }
        runtimeModels.remove(key);
        runtimeInfos.remove(key);
    }

    public List<ModelDataSourceInfo> datasourceNodes() {
        List<ModelDataSourceInfo> nodes = new ArrayList<ModelDataSourceInfo>();
        for (ModelDataSourceInfo info : runtimeInfos.values()) {
            nodes.add(info.simple());
        }
        return nodes;
    }

    public LangChain4jModel getModel(String key) {
        LangChain4jModel model = runtimeModels.get(key);
        if (model != null) {
            return model;
        }
        return springBootRegistry == null ? null : springBootRegistry.get(key);
    }

    public LangChain4jModel getDefault() {
        return springBootRegistry == null ? null : springBootRegistry.getDefault();
    }

    public boolean contains(String key) {
        return runtimeModels.containsKey(key) || (springBootRegistry != null && springBootRegistry.contains(key));
    }

    public Set<String> keys() {
        Set<String> keys = new LinkedHashSet<String>();
        if (springBootRegistry != null) {
            keys.addAll(springBootRegistry.keys());
        }
        keys.addAll(runtimeModels.keySet());
        return Collections.unmodifiableSet(keys);
    }

    private void validate(ModelDataSourceInfo info) {
        if (info == null) {
            throw new LangChain4jModelException("Model datasource must not be null");
        }
        if (isBlank(info.getKey())) {
            throw new LangChain4jModelException("Model datasource key must not be blank");
        }
        if (isBlank(info.getProvider())) {
            throw new LangChain4jModelException("Model datasource provider must not be blank: " + info.getKey());
        }
        if (info.getModelType() == null) {
            throw new LangChain4jModelException("Model datasource model type must not be null: " + info.getKey());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
