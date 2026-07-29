package com.xiongx9527.fw.plus.ai.store.embedding;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultAiEmbeddingStoreFactoryTest {

    @Test
    public void shouldUseOnlyStoreAdapterWhenFrameworkIsBlank() {
        AiEmbeddingStore store = new TestStore();
        AiEmbeddingStoreAdapter supported = new TestAdapter("framework-a", true, store);
        AiEmbeddingStoreAdapter unsupported = new TestAdapter("framework-b", false, new TestStore());

        AiEmbeddingStore resolved = new DefaultAiEmbeddingStoreFactory().create(blankFrameworkDefinition(),
                Arrays.asList(unsupported, supported));

        assertSame(store, resolved);
    }

    @Test
    public void shouldRejectAmbiguousStoreAdapters() {
        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultAiEmbeddingStoreFactory().create(blankFrameworkDefinition(), Arrays.asList(
                        new TestAdapter("framework-b", true, new TestStore()),
                        new TestAdapter("framework-a", true, new TestStore()))));

        assertContextAndCandidates(exception, "framework-a", "framework-b");
    }

    @Test
    public void shouldIncludeStoreDefinitionContextInFailure() {
        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultAiEmbeddingStoreFactory().create(definition("missing-framework"),
                        Collections.<AiEmbeddingStoreAdapter>singletonList(
                                new TestAdapter("other-framework", true, new TestStore()))));

        assertContextAndCandidates(exception, "other-framework");
        assertFalse(exception.getMessage().contains("top-secret-password"));
    }

    @Test
    public void shouldUseOnlyStrictlyMatchingStoreFramework() {
        AiEmbeddingStore store = new TestStore();
        AiEmbeddingStoreAdapter matching = new TestAdapter("framework-a", true, store);
        AiEmbeddingStoreAdapter differentCase = new TestAdapter("FRAMEWORK-A", true, new TestStore());

        AiEmbeddingStore resolved = new DefaultAiEmbeddingStoreFactory().create(definition("framework-a"),
                Arrays.asList(differentCase, matching));

        assertSame(store, resolved);
    }

    private AiEmbeddingStoreDefinition blankFrameworkDefinition() {
        return definition(" ");
    }

    private AiEmbeddingStoreDefinition definition(String framework) {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey("knowledge");
        definition.setFramework(framework);
        definition.setProvider("test-provider");
        definition.setType(AiEmbeddingStoreType.VECTOR);
        definition.setProperties(Collections.<String, Object>singletonMap("password", "top-secret-password"));
        return definition;
    }

    private void assertContextAndCandidates(AiEmbeddingStoreException exception, String... frameworks) {
        String message = exception.getMessage();
        assertTrue(message.contains("key=knowledge"));
        assertTrue(message.contains("framework="));
        assertTrue(message.contains("provider=test-provider"));
        assertTrue(message.contains("type=VECTOR"));
        assertTrue(message.contains("candidateFrameworks="));
        for (String framework : frameworks) {
            assertTrue(message.contains(framework));
        }
    }

    private static class TestAdapter implements AiEmbeddingStoreAdapter {

        private final String framework;

        private final boolean supported;

        private final AiEmbeddingStore store;

        private TestAdapter(String framework, boolean supported, AiEmbeddingStore store) {
            this.framework = framework;
            this.supported = supported;
            this.store = store;
        }

        @Override
        public String getFramework() {
            return framework;
        }

        @Override
        public boolean supports(AiEmbeddingStoreDefinition definition) {
            return supported;
        }

        @Override
        public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition) {
            return store;
        }
    }

    private static class TestStore implements AiEmbeddingStore {

        @Override
        public String getKey() {
            return "knowledge";
        }

        @Override
        public String getFramework() {
            return "test";
        }

        @Override
        public String getProvider() {
            return "test-provider";
        }

        @Override
        public AiEmbeddingStoreType getType() {
            return AiEmbeddingStoreType.VECTOR;
        }

        @Override
        public String add(AiEmbeddingDocument document) {
            return document.getId();
        }

        @Override
        public List<String> addAll(List<AiEmbeddingDocument> documents) {
            return Collections.emptyList();
        }

        @Override
        public void remove(String id) {
        }

        @Override
        public void removeAll(List<String> ids) {
        }

        @Override
        public AiEmbeddingSearchResult search(AiEmbeddingQuery query) {
            return new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList());
        }
    }
}
