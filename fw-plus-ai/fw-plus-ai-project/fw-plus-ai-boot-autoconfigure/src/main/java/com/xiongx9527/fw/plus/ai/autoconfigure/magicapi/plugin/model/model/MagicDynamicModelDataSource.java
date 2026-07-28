package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelException;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
        validateKey(info);
        ModelDataSourceInfo configuredInfo = configured(info);
        destroyClientByKey(configuredInfo.getKey());
        if (!configuredInfo.isEnabled()) {
            return;
        }
        validateEnabled(configuredInfo);
        ModelDataSourceInfo runtimeInfo = configuredInfo.copy();
        Map<String, LangChain4jModel> models = createModels(configuredInfo);
        runtimeModels.putAll(models);
        runtimeInfos.put(configuredInfo.getKey(), runtimeInfo);
    }

    public void destroyClientByKey(String key) {
        if (isBlank(key)) {
            return;
        }
        runtimeModels.remove(key);
        for (String runtimeKey : new ArrayList<String>(runtimeModels.keySet())) {
            if (runtimeKey.startsWith(key + ".")) {
                runtimeModels.remove(runtimeKey);
            }
        }
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
        if (springBootRegistry == null) {
            return null;
        }
        String defaultKey = springBootRegistry.getDefaultKey();
        if (!isBlank(defaultKey)) {
            return getModel(defaultKey);
        }
        LangChain4jModel springDefault = springBootRegistry.getDefault();
        for (String key : springBootRegistry.keys()) {
            LangChain4jModel springModel = springBootRegistry.get(key);
            if (springModel == springDefault || springModel.equals(springDefault)) {
                return getModel(key);
            }
        }
        return springDefault;
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

    private void validateKey(ModelDataSourceInfo info) {
        if (info == null) {
            throw new LangChain4jModelException("Model datasource must not be null");
        }
        if (isBlank(info.getKey())) {
            throw new LangChain4jModelException("Model datasource key must not be blank");
        }
    }

    private void validateEnabled(ModelDataSourceInfo info) {
        if (isBlank(info.getProvider())) {
            throw new LangChain4jModelException("Model datasource provider must not be blank: " + info.getKey());
        }
        if (info.getModelType() == null) {
            throw new LangChain4jModelException("Model datasource model type must not be null: " + info.getKey());
        }
        List<String> modelNames = info.modelNames();
        if (modelNames.isEmpty()) {
            throw new LangChain4jModelException("Model datasource modelName must not be empty: " + info.getKey());
        }
        for (String modelName : modelNames) {
            if (isBlank(modelName)) {
                throw new LangChain4jModelException("Model datasource modelName must not be blank: " + info.getKey());
            }
        }
        if (modelNames.size() > 1 && !isBlank(info.getDefaultModelName()) && !modelNames.contains(info.getDefaultModelName().trim())) {
            throw new LangChain4jModelException("Model datasource defaultModelName is invalid: "
                    + info.getKey() + "." + info.getDefaultModelName());
        }
    }

    private Map<String, LangChain4jModel> createModels(ModelDataSourceInfo info) {
        List<String> modelNames = info.modelNames();
        if (modelNames.size() <= 1) {
            return Collections.singletonMap(info.getKey(), modelFactory.create(info.toDefinition(), providerAdapters));
        }
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        for (String modelName : modelNames) {
            ModelDataSourceInfo.ModelConfig modelConfig = info.modelConfig(modelName);
            if (modelConfig != null && Boolean.FALSE.equals(modelConfig.getEnabled())) {
                continue;
            }
            String modelKey = info.getKey() + "." + modelName;
            models.put(modelKey, modelFactory.create(info.toDefinition(modelKey, modelName, modelConfig), providerAdapters));
        }
        String defaultModelName = isBlank(info.getDefaultModelName()) ? modelNames.get(0) : info.getDefaultModelName().trim();
        LangChain4jModel defaultModel = models.get(info.getKey() + "." + defaultModelName);
        if (defaultModel == null) {
            throw new LangChain4jModelException("Model datasource defaultModelName is invalid: "
                    + info.getKey() + "." + defaultModelName);
        }
        models.put(info.getKey(), defaultModel);
        return models;
    }

    private ModelDataSourceInfo configured(ModelDataSourceInfo source) {
        ModelDataSourceInfo info = source.copy();
        info.setProvider(config(info, "provider", info.getProvider()));
        info.setModelType(modelType(config(info, "modelType", info.getModelType() == null ? null : info.getModelType().name())));
        info.setBaseUrl(config(info, "baseUrl", info.getBaseUrl()));
        info.setApiKey(config(info, "apiKey", info.getApiKey()));
        info.setSecretKey(config(info, "secretKey", info.getSecretKey()));
        info.setAccessToken(config(info, "accessToken", info.getAccessToken()));
        info.setToken(config(info, "token", info.getToken()));
        info.setPassword(config(info, "password", info.getPassword()));
        info.setProperties(MagicAPIConfigUtils.mergeProperties(info.getProperties(), config(info, "properties", null)));
        return info;
    }

    private LangChain4jModelType modelType(String value) {
        if (isBlank(value)) {
            return null;
        }
        return LangChain4jModelType.valueOf(value.trim().toUpperCase());
    }

    private String config(ModelDataSourceInfo info, String configKey, String defaultValue) {
        return MagicAPIConfigUtils.getModelConfig(info.getKey(), configKey, defaultValue);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
