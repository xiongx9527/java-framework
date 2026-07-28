package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.legacy;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

public class LegacyVectorModuleSupport {

    private final EmbeddingStoreRegistry registry;
    private final String provider;

    public LegacyVectorModuleSupport(EmbeddingStoreRegistry registry, String provider) {
        this.registry = registry;
        this.provider = provider;
    }

    public VectorStoreOperations get(String key) {
        if (registry == null) {
            throw registryUnavailable(key);
        }
        try {
            if (!registry.contains(key)) {
                throw missingKey(key);
            }
            return operations(registry.get(key));
        } catch (MagicAPIVectorException e) {
            throw e;
        } catch (RuntimeException e) {
            throw resolveFailed(key, e);
        }
    }

    public VectorStoreOperations getDefault() {
        if (registry == null) {
            throw registryUnavailable("default");
        }
        try {
            return operations(registry.getDefault());
        } catch (RuntimeException e) {
            if (isDefaultNotConfigured(e)) {
                throw defaultNotConfigured(e);
            }
            throw resolveFailed("default", e);
        }
    }

    public static MagicAPIVectorException unsupportedBuilder(String provider) {
        return new MagicAPIVectorException(deprecationMessage(provider, "key")
                + " Legacy builder parameters are no longer used to create store; configure a named store and access vector['key'].", null);
    }

    private VectorStoreOperations operations(EmbeddingStore<TextSegment> embeddingStore) {
        return new VectorStoreOperations(new LegacyStoreFacade(provider, embeddingStore));
    }

    private static final class LegacyStoreFacade implements AiEmbeddingStore {

        private final String provider;
        private final EmbeddingStore<TextSegment> store;

        private LegacyStoreFacade(String provider, EmbeddingStore<TextSegment> store) {
            this.provider = provider;
            this.store = store;
        }

        @Override public String getKey() { return "legacy"; }
        @Override public String getFramework() { return "langchain4j"; }
        @Override public String getProvider() { return provider; }
        @Override public AiEmbeddingStoreType getType() { return AiEmbeddingStoreType.VECTOR; }
        @Override public String add(AiEmbeddingDocument document) { throw unsupported(); }
        @Override public List<String> addAll(List<AiEmbeddingDocument> documents) { throw unsupported(); }
        @Override public void remove(String id) { store.remove(id); }
        @Override public void removeAll(List<String> ids) { store.removeAll(ids); }
        @Override public AiEmbeddingSearchResult search(AiEmbeddingQuery query) { throw unsupported(); }

        private AiEmbeddingStoreException unsupported() {
            return new AiEmbeddingStoreException("Legacy vector facade only supports removal; migrate to the neutral registry");
        }
    }

    private MagicAPIVectorException missingKey(String key) {
        return new MagicAPIVectorException(deprecationMessage(provider, key) + " Missing key: " + key, null);
    }

    private MagicAPIVectorException registryUnavailable(String key) {
        return new MagicAPIVectorException(deprecationMessage(provider, key) + " Unified vector store registry is unavailable.", null);
    }

    private MagicAPIVectorException defaultNotConfigured(RuntimeException cause) {
        return new MagicAPIVectorException(deprecationMessage(provider, "default") + " Default vector store is not configured.", cause);
    }

    private MagicAPIVectorException resolveFailed(String key, RuntimeException cause) {
        return new MagicAPIVectorException(deprecationMessage(provider, key) + " Vector store resolve failed for key '" + key + "'.", cause);
    }

    private boolean isDefaultNotConfigured(RuntimeException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase().contains("default embedding store")
                && (message.toLowerCase().contains("not configured") || message.toLowerCase().contains("invalid"));
    }

    private static String deprecationMessage(String provider, String key) {
        return "Legacy Magic-API vector provider '" + provider + "' is deprecated; migrate script to import vector and access vector['"
                + key + "'].";
    }
}
