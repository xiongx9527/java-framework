package com.xiongx9527.fw.plus.ai.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultLangChain4jEmbeddingStoreAdapter implements AiEmbeddingStoreAdapter {

    private static final String FRAMEWORK = "langchain4j";

    private final Map<String, LangChain4jEmbeddingStoreProviderAdapter> providerAdapters;

    public DefaultLangChain4jEmbeddingStoreAdapter() {
        this(Collections.<String, LangChain4jEmbeddingStoreProviderAdapter>emptyMap());
    }

    public DefaultLangChain4jEmbeddingStoreAdapter(
            Map<String, ? extends LangChain4jEmbeddingStoreProviderAdapter> providerAdapters) {
        Map<String, LangChain4jEmbeddingStoreProviderAdapter> copy =
                new LinkedHashMap<String, LangChain4jEmbeddingStoreProviderAdapter>();
        if (providerAdapters != null) {
            copy.putAll(providerAdapters);
        }
        this.providerAdapters = Collections.unmodifiableMap(copy);
    }

    @Override
    public String getFramework() {
        return FRAMEWORK;
    }

    @Override
    public boolean supports(AiEmbeddingStoreDefinition definition) {
        if (!supportsFrameworkAndType(definition)) {
            return false;
        }
        return matchingAdapters(definition).size() == 1;
    }

    @Override
    public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition) {
        if (!supportsFrameworkAndType(definition)) {
            throw new AiEmbeddingStoreException("LangChain4j adapter does not support store: " + context(definition));
        }
        List<ProviderCandidate> candidates = matchingAdapters(definition);
        if (candidates.isEmpty()) {
            throw new AiEmbeddingStoreException("No LangChain4j provider adapter supports store: "
                    + context(definition));
        }
        if (candidates.size() > 1) {
            throw new AiEmbeddingStoreException("Multiple LangChain4j provider adapters support store: "
                    + context(definition) + ", candidates=" + candidateNames(candidates));
        }
        ProviderCandidate candidate = candidates.get(0);
        EmbeddingStore<TextSegment> nativeStore;
        try {
            nativeStore = candidate.adapter.create(definition);
        } catch (Exception e) {
            throw new AiEmbeddingStoreException("LangChain4j provider adapter failed to create store: "
                    + context(definition) + ", candidate=" + candidate.name
                    + ", causeType=" + e.getClass().getName(), e);
        }
        if (nativeStore == null) {
            throw new AiEmbeddingStoreException("LangChain4j provider adapter returned null store: "
                    + context(definition) + ", candidate=" + candidate.name);
        }
        return new LangChain4jAiEmbeddingStore(definition, nativeStore);
    }

    private boolean supportsFrameworkAndType(AiEmbeddingStoreDefinition definition) {
        return definition != null
                && (isBlank(definition.getFramework()) || FRAMEWORK.equalsIgnoreCase(definition.getFramework()))
                && AiEmbeddingStoreType.VECTOR == definition.getType();
    }

    private List<ProviderCandidate> matchingAdapters(AiEmbeddingStoreDefinition definition) {
        List<ProviderCandidate> matches = new ArrayList<ProviderCandidate>();
        Set<LangChain4jEmbeddingStoreProviderAdapter> discovered = Collections.newSetFromMap(
                new IdentityHashMap<LangChain4jEmbeddingStoreProviderAdapter, Boolean>());
        for (Map.Entry<String, LangChain4jEmbeddingStoreProviderAdapter> entry : providerAdapters.entrySet()) {
            LangChain4jEmbeddingStoreProviderAdapter adapter = entry.getValue();
            if (adapter == null || !discovered.add(adapter)) {
                continue;
            }
            boolean supported;
            try {
                supported = adapter.supports(definition);
            } catch (Exception e) {
                throw new AiEmbeddingStoreException("LangChain4j provider adapter failed while checking store support: "
                        + context(definition) + ", candidate=" + entry.getKey()
                        + ", causeType=" + e.getClass().getName(), e);
            }
            if (supported) {
                matches.add(new ProviderCandidate(entry.getKey(), adapter));
            }
        }
        if (matches.size() > 1) {
            throw new AiEmbeddingStoreException("Multiple LangChain4j provider adapters support store: "
                    + context(definition) + ", candidates=" + candidateNames(matches));
        }
        return matches;
    }

    private List<String> candidateNames(List<ProviderCandidate> candidates) {
        List<String> names = new ArrayList<String>();
        for (ProviderCandidate candidate : candidates) {
            names.add(candidate.name);
        }
        Collections.sort(names);
        return names;
    }

    private String context(AiEmbeddingStoreDefinition definition) {
        if (definition == null) {
            return "key=unknown, framework=unknown, provider=unknown, type=unknown";
        }
        return "key=" + value(definition.getKey())
                + ", framework=" + value(definition.getFramework())
                + ", provider=" + value(definition.getProvider())
                + ", type=" + value(definition.getType());
    }

    private String value(Object value) {
        return value == null ? "unknown" : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static final class ProviderCandidate {

        private final String name;
        private final LangChain4jEmbeddingStoreProviderAdapter adapter;

        private ProviderCandidate(String name, LangChain4jEmbeddingStoreProviderAdapter adapter) {
            this.name = name;
            this.adapter = adapter;
        }
    }
}
