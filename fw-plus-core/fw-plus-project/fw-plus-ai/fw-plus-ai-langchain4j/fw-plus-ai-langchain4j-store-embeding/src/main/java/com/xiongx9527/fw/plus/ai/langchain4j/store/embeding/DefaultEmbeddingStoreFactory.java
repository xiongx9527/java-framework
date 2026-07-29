package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreFactory;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultEmbeddingStoreFactory implements EmbeddingStoreFactory, AiEmbeddingStoreAdapter {

    private static final String FRAMEWORK = "langchain4j";
    private static final String DIMENSION = "dimension";
    private static final String METADATA_KEYS = "metadataKeys";

    private final List<EmbeddingStoreProviderAdapter> providerAdapters;
    private final FailureCapture failureCapture;

    public DefaultEmbeddingStoreFactory() {
        this(Collections.<EmbeddingStoreProviderAdapter>emptyList());
    }

    public DefaultEmbeddingStoreFactory(List<EmbeddingStoreProviderAdapter> providerAdapters) {
        this(providerAdapters, null);
    }

    private DefaultEmbeddingStoreFactory(List<EmbeddingStoreProviderAdapter> providerAdapters,
                                         FailureCapture failureCapture) {
        this.providerAdapters = providerAdapters == null
                ? Collections.<EmbeddingStoreProviderAdapter>emptyList()
                : Collections.unmodifiableList(new ArrayList<EmbeddingStoreProviderAdapter>(providerAdapters));
        this.failureCapture = failureCapture;
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition,
                                              List<EmbeddingStoreProviderAdapter> adapters) {
        FailureCapture capture = new FailureCapture();
        try {
            AiEmbeddingStore store = new DefaultAiEmbeddingStoreFactory().create(toNeutral(definition),
                    Collections.<AiEmbeddingStoreAdapter>singletonList(
                            new DefaultEmbeddingStoreFactory(adapters, capture)));
            return ((LangChain4jAiEmbeddingStore) store).nativeStore();
        } catch (AiEmbeddingStoreException e) {
            throw new EmbeddingStoreException(e.getMessage(),
                    capture.getFailure() == null ? e.getCause() : capture.getFailure());
        }
    }

    @Override
    public String getFramework() {
        return FRAMEWORK;
    }

    @Override
    public boolean supports(AiEmbeddingStoreDefinition definition) {
        try {
            return definition != null
                    && FRAMEWORK.equalsIgnoreCase(definition.getFramework())
                    && findAdapter(definition.getProvider()) != null;
        } catch (RuntimeException e) {
            capture(e);
            throw e;
        }
    }

    @Override
    public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition) {
        if (definition == null || !FRAMEWORK.equalsIgnoreCase(definition.getFramework())) {
            throw new AiEmbeddingStoreException("LangChain4j store adapter does not support definition");
        }
        EmbeddingStoreProviderAdapter adapter = findAdapter(definition.getProvider());
        if (adapter == null) {
            throw new AiEmbeddingStoreException("No LangChain4j store provider adapter supports " + context(definition));
        }
        EmbeddingStore<TextSegment> nativeStore;
        try {
            nativeStore = adapter.create(toLegacy(definition));
        } catch (RuntimeException e) {
            capture(e);
            throw new AiEmbeddingStoreException("LangChain4j store provider adapter failed: "
                    + context(definition) + ", causeType=" + e.getClass().getName(), e);
        }
        if (nativeStore == null) {
            capture(new IllegalStateException("Adapter returned null embedding store"));
            throw new AiEmbeddingStoreException("LangChain4j store provider adapter returned null: "
                    + context(definition));
        }
        return new LangChain4jAiEmbeddingStore(definition, nativeStore);
    }

    private EmbeddingStoreProviderAdapter findAdapter(String provider) {
        for (EmbeddingStoreProviderAdapter adapter : providerAdapters) {
            if (adapter != null && adapter.supports(provider)) {
                return adapter;
            }
        }
        return null;
    }

    static AiEmbeddingStoreDefinition toNeutral(EmbeddingStoreDefinition definition) {
        if (definition == null) {
            return null;
        }
        Map<String, Object> properties = definition.getProperties() == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(definition.getProperties());
        if (definition.getDimension() != null) {
            properties.put(DIMENSION, definition.getDimension());
        }
        if (definition.getMetadataKeys() != null && !definition.getMetadataKeys().isEmpty()) {
            properties.put(METADATA_KEYS, new ArrayList<String>(definition.getMetadataKeys()));
        }
        AiEmbeddingStoreDefinition neutral = new AiEmbeddingStoreDefinition();
        neutral.setKey(definition.getKey());
        neutral.setFramework(FRAMEWORK);
        neutral.setProvider(definition.getProvider());
        neutral.setType(AiEmbeddingStoreType.VECTOR);
        neutral.setEnabled(definition.isEnabled());
        neutral.setProperties(properties);
        return neutral;
    }

    static EmbeddingStoreDefinition toLegacy(AiEmbeddingStoreDefinition definition) {
        EmbeddingStoreDefinition legacy = new EmbeddingStoreDefinition();
        legacy.setKey(definition.getKey());
        legacy.setProvider(definition.getProvider());
        legacy.setEnabled(definition.isEnabled());
        Map<String, Object> properties = definition.getProperties() == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(definition.getProperties());
        Object dimension = properties.remove(DIMENSION);
        if (dimension instanceof Number) {
            legacy.setDimension(((Number) dimension).intValue());
        } else if (dimension != null) {
            legacy.setDimension(Integer.valueOf(String.valueOf(dimension)));
        }
        Object metadataKeys = properties.remove(METADATA_KEYS);
        if (metadataKeys instanceof Collection<?>) {
            List<String> keys = new ArrayList<String>();
            for (Object key : (Collection<?>) metadataKeys) {
                keys.add(String.valueOf(key));
            }
            legacy.setMetadataKeys(keys);
        }
        legacy.setProperties(properties);
        return legacy;
    }

    private static String context(AiEmbeddingStoreDefinition definition) {
        return "key=" + definition.getKey()
                + ", framework=" + definition.getFramework()
                + ", provider=" + definition.getProvider()
                + ", type=" + definition.getType();
    }

    private void capture(RuntimeException failure) {
        if (failureCapture != null) {
            failureCapture.capture(failure);
        }
    }

    private static final class FailureCapture {

        private RuntimeException failure;

        private void capture(RuntimeException failure) {
            if (this.failure == null) {
                this.failure = failure;
            }
        }

        private RuntimeException getFailure() {
            return failure;
        }
    }
}
