package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class DefaultAiEmbeddingStoreFactory implements AiEmbeddingStoreFactory {

    @Override
    public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition, List<AiEmbeddingStoreAdapter> adapters) {
        List<AdapterCandidate> candidates = candidates(adapters, definition);
        validateDefinition(definition, candidates);
        List<AdapterCandidate> supported = new ArrayList<AdapterCandidate>();
        for (AdapterCandidate candidate : candidates) {
            if (matchesFramework(definition, candidate) && supports(candidate.adapter, definition, candidates)) {
                supported.add(candidate);
            }
        }
        if (supported.size() == 1) {
            return create(supported.get(0).adapter, definition, candidates);
        }
        if (supported.isEmpty()) {
            throw new AiEmbeddingStoreException("No adapter supports store: " + context(definition, candidates));
        }
        throw new AiEmbeddingStoreException("Multiple adapters support store: " + context(definition, candidates));
    }

    private void validateDefinition(AiEmbeddingStoreDefinition definition, List<AdapterCandidate> candidates) {
        if (definition == null) {
            throw new AiEmbeddingStoreException("Store definition must not be null: " + context(null, candidates));
        }
        if (isBlank(definition.getKey())) {
            throw new AiEmbeddingStoreException("Store definition key must not be blank: " + context(definition, candidates));
        }
        if (isBlank(definition.getProvider())) {
            throw new AiEmbeddingStoreException("Store definition provider must not be blank: " + context(definition, candidates));
        }
        if (definition.getType() == null) {
            throw new AiEmbeddingStoreException("Store definition type must not be null: " + context(definition, candidates));
        }
    }

    private List<AdapterCandidate> candidates(List<AiEmbeddingStoreAdapter> adapters,
                                              AiEmbeddingStoreDefinition definition) {
        List<AdapterCandidate> candidates = new ArrayList<AdapterCandidate>();
        if (adapters == null) {
            return candidates;
        }
        Set<AiEmbeddingStoreAdapter> discovered = Collections.newSetFromMap(
                new IdentityHashMap<AiEmbeddingStoreAdapter, Boolean>());
        for (AiEmbeddingStoreAdapter adapter : adapters) {
            if (adapter != null) {
                String framework = framework(adapter, definition, candidates);
                if (discovered.add(adapter)) {
                    candidates.add(new AdapterCandidate(adapter, framework));
                }
            }
        }
        return candidates;
    }

    private String framework(AiEmbeddingStoreAdapter adapter, AiEmbeddingStoreDefinition definition,
                             List<AdapterCandidate> candidates) {
        try {
            return adapter.getFramework();
        } catch (Exception e) {
            throw new AiEmbeddingStoreException("Adapter failed while reading store framework: "
                    + context(definition, candidates) + ", causeType=" + e.getClass().getName());
        }
    }

    private boolean matchesFramework(AiEmbeddingStoreDefinition definition, AdapterCandidate candidate) {
        return isBlank(definition.getFramework()) || definition.getFramework().equals(candidate.framework);
    }

    private boolean supports(AiEmbeddingStoreAdapter adapter, AiEmbeddingStoreDefinition definition,
                             List<AdapterCandidate> candidates) {
        try {
            return adapter.supports(definition);
        } catch (Exception e) {
            throw new AiEmbeddingStoreException("Adapter failed while checking store support: "
                    + context(definition, candidates) + ", causeType=" + e.getClass().getName());
        }
    }

    private AiEmbeddingStore create(AiEmbeddingStoreAdapter adapter, AiEmbeddingStoreDefinition definition,
                                    List<AdapterCandidate> candidates) {
        AiEmbeddingStore store;
        try {
            store = adapter.create(definition);
        } catch (Exception e) {
            throw new AiEmbeddingStoreException("Adapter failed to create store: "
                    + context(definition, candidates) + ", causeType=" + e.getClass().getName());
        }
        if (store == null) {
            throw new AiEmbeddingStoreException("Adapter returned null store: " + context(definition, candidates));
        }
        return store;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static String context(AiEmbeddingStoreDefinition definition) {
        return context(definition == null ? null : definition.getKey(), definition);
    }

    static String context(String key, AiEmbeddingStoreDefinition definition) {
        return "key=" + value(key)
                + ", framework=" + value(definition == null ? null : definition.getFramework())
                + ", provider=" + value(definition == null ? null : definition.getProvider())
                + ", type=" + value(definition == null ? null : definition.getType());
    }

    private static String context(AiEmbeddingStoreDefinition definition, List<AdapterCandidate> candidates) {
        return context(definition) + ", candidateFrameworks=" + candidateFrameworks(candidates);
    }

    private static List<String> candidateFrameworks(List<AdapterCandidate> candidates) {
        List<String> frameworks = new ArrayList<String>();
        for (AdapterCandidate candidate : candidates) {
            frameworks.add(value(candidate.framework));
        }
        Collections.sort(frameworks);
        return frameworks;
    }

    private static String value(Object value) {
        return value == null ? "unknown" : String.valueOf(value);
    }

    private static final class AdapterCandidate {

        private final AiEmbeddingStoreAdapter adapter;

        private final String framework;

        private AdapterCandidate(AiEmbeddingStoreAdapter adapter, String framework) {
            this.adapter = adapter;
            this.framework = framework;
        }
    }
}
