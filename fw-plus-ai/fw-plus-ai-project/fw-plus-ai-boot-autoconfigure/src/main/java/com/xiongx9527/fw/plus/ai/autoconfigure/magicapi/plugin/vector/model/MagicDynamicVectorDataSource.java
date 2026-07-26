package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreResolver;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MagicDynamicVectorDataSource implements VectorStoreResolver {

    private static final String[] CONFIG_PROPERTY_KEYS = new String[] {
            "host", "port", "uri", "collectionName", "apiKey", "token", "username", "password", "databaseName",
            "indexType", "metricType", "consistencyLevel", "retrieveEmbeddingsOnSearch", "autoFlushOnInsert",
            "payloadTextKey", "useTls", "utls", "scheme", "className", "useGrpcForInserts", "securedGrpc",
            "grpcPort", "avoidDups", "textFieldName"
    };

    private final EmbeddingStoreRegistry springBootRegistry;

    private final EmbeddingStoreFactory embeddingStoreFactory;

    private final List<EmbeddingStoreProviderAdapter> providerAdapters;

    private final Map<String, EmbeddingStore<TextSegment>> runtimeStores = new ConcurrentHashMap<String, EmbeddingStore<TextSegment>>();

    private final Map<String, VectorDataSourceInfo> runtimeInfos = new ConcurrentHashMap<String, VectorDataSourceInfo>();

    public MagicDynamicVectorDataSource(EmbeddingStoreRegistry springBootRegistry,
                                        EmbeddingStoreFactory embeddingStoreFactory,
                                        List<EmbeddingStoreProviderAdapter> providerAdapters) {
        this.springBootRegistry = springBootRegistry;
        this.embeddingStoreFactory = embeddingStoreFactory;
        this.providerAdapters = providerAdapters == null ? Collections.<EmbeddingStoreProviderAdapter>emptyList() : providerAdapters;
    }

    public void createClient(VectorDataSourceInfo info) {
        validate(info);
        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreFactory.create(definition(info), providerAdapters);
        runtimeStores.put(info.getKey(), embeddingStore);
        runtimeInfos.put(info.getKey(), copy(info));
    }

    public void destroyClientByKey(String key) {
        if (isBlank(key)) {
            return;
        }
        runtimeStores.remove(key);
        runtimeInfos.remove(key);
    }

    public List<VectorDataSourceInfo> datasourceNodes() {
        List<VectorDataSourceInfo> nodes = new ArrayList<VectorDataSourceInfo>(runtimeInfos.values());
        if (hasSpringBootDefaultStore()) {
            VectorDataSourceInfo info = new VectorDataSourceInfo();
            info.setKey("default");
            info.setName("默认数据源");
            nodes.add(info);
        }
        return nodes;
    }

    public EmbeddingStore<TextSegment> get(String key) {
        EmbeddingStore<TextSegment> runtimeStore = runtimeStores.get(key);
        if (runtimeStore != null) {
            return runtimeStore;
        }
        return springBootRegistry.get(key);
    }

    public EmbeddingStore<TextSegment> getDefault() {
        return springBootRegistry.getDefault();
    }

    public boolean contains(String key) {
        return runtimeStores.containsKey(key) || springBootRegistry.contains(key);
    }

    public Set<String> keys() {
        Set<String> keys = new LinkedHashSet<String>();
        keys.addAll(springBootRegistry.keys());
        keys.addAll(runtimeStores.keySet());
        return Collections.unmodifiableSet(keys);
    }

    private VectorDataSourceInfo copy(VectorDataSourceInfo source) {
        VectorDataSourceInfo target = new VectorDataSourceInfo();
        target.setName(source.getName());
        target.setKey(source.getKey());
        target.setProvider(source.getProvider());
        target.setEnabled(source.isEnabled());
        target.setDimension(source.getDimension());
        target.setMetadataKeys(new ArrayList<String>(source.getMetadataKeys()));
        target.setProperties(source.getProperties());
        return target;
    }

    private EmbeddingStoreDefinition definition(VectorDataSourceInfo info) {
        EmbeddingStoreDefinition definition = new EmbeddingStoreDefinition();
        definition.setKey(info.getKey());
        definition.setProvider(info.getProvider());
        definition.setEnabled(info.isEnabled());
        definition.setDimension(integerConfig(info, "dimension", info.getDimension()));
        definition.setMetadataKeys(metadataKeys(info));
        definition.setProperties(properties(info));
        return definition;
    }

    private Map<String, Object> properties(VectorDataSourceInfo info) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        if (info.getProperties() != null) {
            properties.putAll(info.getProperties());
        }
        mergeJsonProperties(properties, config(info, "properties", properties.isEmpty() ? null : JSON.toJSONString(properties)));
        for (String propertyKey : CONFIG_PROPERTY_KEYS) {
            String value = config(info, propertyKey, stringValue(properties.get(propertyKey)));
            if (!isBlank(value)) {
                properties.put(propertyKey, value);
            }
        }
        return properties;
    }

    private void mergeJsonProperties(Map<String, Object> properties, String json) {
        if (isBlank(json)) {
            return;
        }
        properties.putAll(JSON.parseObject(json, new TypeReference<Map<String, Object>>() {
        }));
    }

    private List<String> metadataKeys(VectorDataSourceInfo info) {
        String value = config(info, "metadataKeys", JSON.toJSONString(info.getMetadataKeys()));
        if (isBlank(value)) {
            return new ArrayList<String>(info.getMetadataKeys());
        }
        return JSON.parseObject(value, new TypeReference<List<String>>() {
        });
    }

    private Integer integerConfig(VectorDataSourceInfo info, String configKey, Integer defaultValue) {
        String value = config(info, configKey, defaultValue == null ? null : String.valueOf(defaultValue));
        if (isBlank(value)) {
            return defaultValue;
        }
        return Integer.valueOf(value);
    }

    private String config(VectorDataSourceInfo info, String configKey, String defaultValue) {
        String provider = info.getProvider();
        if ("milvus".equalsIgnoreCase(provider)) {
            return MagicAPIConfigUtils.getMilvusConfig(info.getKey(), configKey, defaultValue);
        }
        if ("qdrant".equalsIgnoreCase(provider)) {
            return MagicAPIConfigUtils.getQdrantConfig(info.getKey(), configKey, defaultValue);
        }
        if ("weaviate".equalsIgnoreCase(provider)) {
            return MagicAPIConfigUtils.getWeaviateConfig(info.getKey(), configKey, defaultValue);
        }
        return defaultValue;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void validate(VectorDataSourceInfo definition) {
        if (definition == null) {
            throw new EmbeddingStoreException("Vector datasource must not be null");
        }
        if (isBlank(definition.getKey())) {
            throw new EmbeddingStoreException("Vector datasource key must not be blank");
        }
        if (isBlank(definition.getProvider())) {
            throw new EmbeddingStoreException("Vector datasource provider must not be blank: " + definition.getKey());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private boolean hasSpringBootDefaultStore() {
        try {
            return springBootRegistry != null && !springBootRegistry.keys().isEmpty();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
