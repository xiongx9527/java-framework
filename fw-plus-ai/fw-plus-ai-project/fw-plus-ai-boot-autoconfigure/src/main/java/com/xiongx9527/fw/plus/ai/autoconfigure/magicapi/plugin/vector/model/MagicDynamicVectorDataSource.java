package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MagicDynamicVectorDataSource implements VectorStoreResolver {

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
        definition.setDimension(info.getDimension());
        definition.setMetadataKeys(new ArrayList<String>(info.getMetadataKeys()));
        definition.setProperties(info.getProperties());
        return definition;
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
