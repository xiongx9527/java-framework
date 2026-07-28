package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MagicDynamicModelDataSource {

    private final AiModelRegistry springBootRegistry;
    private final AiModelFactory modelFactory;
    private final List<AiModelAdapter> providerAdapters;
    private final Map<String, AiModel> runtimeModels = new ConcurrentHashMap<String, AiModel>();
    private final Map<String, ModelDataSourceInfo> runtimeInfos = new ConcurrentHashMap<String, ModelDataSourceInfo>();

    public MagicDynamicModelDataSource(AiModelRegistry springBootRegistry,
                                       AiModelFactory modelFactory,
                                       List<AiModelAdapter> providerAdapters) {
        this.springBootRegistry = springBootRegistry;
        this.modelFactory = modelFactory;
        this.providerAdapters = providerAdapters == null ? Collections.<AiModelAdapter>emptyList() : providerAdapters;
    }

    /** @deprecated 生产自动配置已改用中立接口，仅保留旧源码的构造兼容。 */
    @Deprecated
    public MagicDynamicModelDataSource(
            com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry registry,
            final com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory factory,
            final List<com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter> adapters) {
        this(legacyRegistry(registry), new AiModelFactory() {
            @Override
            public AiModel create(AiModelDefinition definition, List<AiModelAdapter> ignored) {
                com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition legacy = legacy(definition);
                return facade(definition.getKey(), factory.create(legacy, adapters));
            }
        }, Collections.<AiModelAdapter>emptyList());
    }

    private static AiModelRegistry legacyRegistry(
            final com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry registry) {
        if (registry == null) {
            return null;
        }
        return new AiModelRegistry() {
            @Override public AiModel get(String key) { return facade(key, registry.get(key)); }
            @Override public AiModel getDefault() { return facade(registry.getDefaultKey(), registry.getDefault()); }
            @Override public String getDefaultKey() { return registry.getDefaultKey(); }
            @Override public boolean contains(String key) { return registry.contains(key); }
            @Override public Set<String> keys() { return registry.keys(); }
        };
    }

    private static com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition legacy(
            AiModelDefinition definition) {
        com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition legacy =
                new com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition();
        legacy.setKey(definition.getKey());
        legacy.setProvider(definition.getProvider());
        legacy.setModelType(definition.getType() == null ? null : LangChain4jModelType.valueOf(definition.getType().name()));
        legacy.setEnabled(definition.isEnabled());
        Map<String, Object> properties = new LinkedHashMap<String, Object>(definition.getProperties());
        legacy.setBaseUrl(value(properties.remove("baseUrl")));
        legacy.setApiKey(value(properties.remove("apiKey")));
        legacy.setModelName(value(properties.remove("modelName")));
        legacy.setProperties(properties);
        return legacy;
    }

    private static AiModel facade(final String key,
                                  final com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel model) {
        if (model == null) {
            return null;
        }
        return new AiModel() {
            @Override public String getKey() { return key; }
            @Override public String getFramework() { return "langchain4j"; }
            @Override public String getProvider() { return model.getProvider(); }
            @Override public AiModelType getType() {
                return model.getModelType() == null ? null : AiModelType.valueOf(model.getModelType().name());
            }
        };
    }

    private static String value(Object value) {
        return value == null ? null : String.valueOf(value);
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
        Map<String, AiModel> models = createModels(configuredInfo);
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

    public AiModel getModel(String key) {
        AiModel model = runtimeModels.get(key);
        if (model != null) {
            return model;
        }
        return springBootRegistry == null ? null : springBootRegistry.get(key);
    }

    public AiModel getDefault() {
        if (springBootRegistry == null) {
            return null;
        }
        String defaultKey = springBootRegistry.getDefaultKey();
        if (!isBlank(defaultKey)) {
            return getModel(defaultKey);
        }
        AiModel springDefault = springBootRegistry.getDefault();
        for (String key : springBootRegistry.keys()) {
            AiModel springModel = springBootRegistry.get(key);
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
            throw new AiModelException("Model datasource must not be null");
        }
        if (isBlank(info.getKey())) {
            throw new AiModelException("Model datasource key must not be blank");
        }
    }

    private void validateEnabled(ModelDataSourceInfo info) {
        if (isBlank(info.getProvider())) {
            throw new AiModelException("Model datasource provider must not be blank: " + info.getKey());
        }
        if (info.getModelType() == null) {
            throw new AiModelException("Model datasource model type must not be null: " + info.getKey());
        }
        List<String> modelNames = info.modelNames();
        if (modelNames.isEmpty()) {
            throw new AiModelException("Model datasource modelName must not be empty: " + info.getKey());
        }
        for (String modelName : modelNames) {
            if (isBlank(modelName)) {
                throw new AiModelException("Model datasource modelName must not be blank: " + info.getKey());
            }
        }
        if (modelNames.size() > 1 && !isBlank(info.getDefaultModelName()) && !modelNames.contains(info.getDefaultModelName().trim())) {
            throw new AiModelException("Model datasource defaultModelName is invalid: "
                    + info.getKey() + "." + info.getDefaultModelName());
        }
    }

    private Map<String, AiModel> createModels(ModelDataSourceInfo info) {
        List<String> modelNames = info.modelNames();
        if (modelNames.size() <= 1) {
            return Collections.singletonMap(info.getKey(), modelFactory.create(neutral(info.toDefinition()), providerAdapters));
        }
        Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
        for (String modelName : modelNames) {
            ModelDataSourceInfo.ModelConfig modelConfig = info.modelConfig(modelName);
            if (modelConfig != null && Boolean.FALSE.equals(modelConfig.getEnabled())) {
                continue;
            }
            String modelKey = info.getKey() + "." + modelName;
            models.put(modelKey, modelFactory.create(neutral(info.toDefinition(modelKey, modelName, modelConfig)), providerAdapters));
        }
        String defaultModelName = isBlank(info.getDefaultModelName()) ? modelNames.get(0) : info.getDefaultModelName().trim();
        AiModel defaultModel = models.get(info.getKey() + "." + defaultModelName);
        if (defaultModel == null) {
            throw new AiModelException("Model datasource defaultModelName is invalid: "
                    + info.getKey() + "." + defaultModelName);
        }
        models.put(info.getKey(), defaultModel);
        return models;
    }

    private AiModelDefinition neutral(com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition legacy) {
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey(legacy.getKey());
        definition.setFramework("langchain4j");
        definition.setProvider(legacy.getProvider());
        definition.setType(legacy.getModelType() == null ? null : AiModelType.valueOf(legacy.getModelType().name()));
        definition.setEnabled(legacy.isEnabled());
        Map<String, Object> properties = new LinkedHashMap<String, Object>(legacy.getProperties());
        put(properties, "baseUrl", legacy.getBaseUrl());
        put(properties, "apiKey", legacy.getApiKey());
        put(properties, "modelName", legacy.getModelName());
        definition.setProperties(properties);
        return definition;
    }

    private void put(Map<String, Object> properties, String key, String value) {
        if (value != null) {
            properties.put(key, value);
        }
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
