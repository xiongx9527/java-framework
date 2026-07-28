package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreResolver;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
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

    private final AiEmbeddingStoreRegistry springBootRegistry;

    private final AiEmbeddingStoreFactory embeddingStoreFactory;

    private final List<AiEmbeddingStoreAdapter> providerAdapters;

    private final Map<String, AiEmbeddingStore> runtimeStores = new ConcurrentHashMap<String, AiEmbeddingStore>();

    private final Map<String, VectorDataSourceInfo> runtimeInfos = new ConcurrentHashMap<String, VectorDataSourceInfo>();

    public MagicDynamicVectorDataSource(AiEmbeddingStoreRegistry springBootRegistry,
                                        AiEmbeddingStoreFactory embeddingStoreFactory,
                                        List<AiEmbeddingStoreAdapter> providerAdapters) {
        this.springBootRegistry = springBootRegistry;
        this.embeddingStoreFactory = embeddingStoreFactory;
        this.providerAdapters = providerAdapters == null ? Collections.<AiEmbeddingStoreAdapter>emptyList() : providerAdapters;
    }

    /** @deprecated 生产自动配置已改用中立接口，仅保留旧源码的构造兼容。 */
    @Deprecated
    public MagicDynamicVectorDataSource(
            final com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry registry,
            final com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory factory,
            final List<com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter> adapters) {
        this(legacyRegistry(registry), new AiEmbeddingStoreFactory() {
            @Override
            public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition,
                                           List<AiEmbeddingStoreAdapter> ignored) {
                com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition legacy =
                        new com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition();
                legacy.setKey(definition.getKey());
                legacy.setProvider(definition.getProvider());
                legacy.setEnabled(definition.isEnabled());
                Map<String, Object> properties = new LinkedHashMap<String, Object>(definition.getProperties());
                Object dimension = properties.remove("dimension");
                if (dimension != null) {
                    legacy.setDimension(Integer.valueOf(String.valueOf(dimension)));
                }
                legacy.setProperties(properties);
                return facade(definition.getKey(), definition.getProvider(), factory.create(legacy, adapters));
            }
        }, Collections.<AiEmbeddingStoreAdapter>emptyList());
    }

    private static AiEmbeddingStoreRegistry legacyRegistry(
            final com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry registry) {
        if (registry == null) {
            return null;
        }
        return new AiEmbeddingStoreRegistry() {
            @Override public AiEmbeddingStore get(String key) { return facade(key, "langchain4j", registry.get(key)); }
            @Override public AiEmbeddingStore getDefault() { return facade("default", "langchain4j", registry.getDefault()); }
            @Override public String getDefaultKey() { return null; }
            @Override public boolean contains(String key) { return registry.contains(key); }
            @Override public Set<String> keys() { return registry.keys(); }
        };
    }

    private static AiEmbeddingStore facade(final String key, final String provider,
                                           final EmbeddingStore<TextSegment> store) {
        if (store == null) {
            return null;
        }
        return new AiEmbeddingStore() {
            @Override public String getKey() { return key; }
            @Override public String getFramework() { return "langchain4j"; }
            @Override public String getProvider() { return provider; }
            @Override public AiEmbeddingStoreType getType() { return AiEmbeddingStoreType.VECTOR; }
            @Override public String add(AiEmbeddingDocument document) { throw unsupported(); }
            @Override public List<String> addAll(List<AiEmbeddingDocument> documents) { throw unsupported(); }
            @Override public void remove(String id) { store.remove(id); }
            @Override public void removeAll(List<String> ids) { store.removeAll(ids); }
            @Override public AiEmbeddingSearchResult search(AiEmbeddingQuery query) { throw unsupported(); }
            private AiEmbeddingStoreException unsupported() {
                return new AiEmbeddingStoreException("Legacy constructor facade does not expose native operations");
            }
        };
    }

    public void createClient(VectorDataSourceInfo info) {
        validate(info);
        AiEmbeddingStore embeddingStore = embeddingStoreFactory.create(definition(info), providerAdapters);
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

    public AiEmbeddingStore get(String key) {
        AiEmbeddingStore runtimeStore = runtimeStores.get(key);
        if (runtimeStore != null) {
            return runtimeStore;
        }
        return springBootRegistry.get(key);
    }

    public AiEmbeddingStore getDefault() {
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

    private AiEmbeddingStoreDefinition definition(VectorDataSourceInfo info) {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey(info.getKey());
        definition.setFramework("langchain4j");
        definition.setProvider(info.getProvider());
        definition.setType(AiEmbeddingStoreType.VECTOR);
        definition.setEnabled(info.isEnabled());
        Map<String, Object> properties = properties(info);
        Integer dimension = integerConfig(info, "dimension", info.getDimension());
        if (dimension != null) {
            properties.put("dimension", dimension);
        }
        List<String> metadataKeys = metadataKeys(info);
        if (!metadataKeys.isEmpty()) {
            properties.put("metadataKeys", metadataKeys);
        }
        definition.setProperties(properties);
        return definition;
    }

    private Map<String, Object> properties(VectorDataSourceInfo info) {
        Map<String, Object> properties = MagicAPIConfigUtils.mergeProperties(
                info.getProperties(), config(info, "properties", null));
        for (String propertyKey : CONFIG_PROPERTY_KEYS) {
            String value = config(info, propertyKey, stringValue(properties.get(propertyKey)));
            if (!isBlank(value)) {
                properties.put(propertyKey, value);
            }
        }
        return properties;
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
        return MagicAPIConfigUtils.getVectorConfig(info.getKey(), configKey, defaultValue);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void validate(VectorDataSourceInfo definition) {
        if (definition == null) {
            throw new AiEmbeddingStoreException("Vector datasource must not be null");
        }
        if (isBlank(definition.getKey())) {
            throw new AiEmbeddingStoreException("Vector datasource key must not be blank");
        }
        if (isBlank(definition.getProvider())) {
            throw new AiEmbeddingStoreException("Vector datasource provider must not be blank: " + definition.getKey());
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
