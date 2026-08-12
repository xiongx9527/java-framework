package com.xiongx9527.fw.plus.ai.store.embedding;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultAiEmbeddingStoreRegistryTest {

    @Test
    public void shouldResolveDefaultStoreThroughSupportedAdapter() {
        FakeAdapter adapter = new FakeAdapter();
        DefaultAiEmbeddingStoreRegistry registry = new DefaultAiEmbeddingStoreRegistry(
                Collections.singletonList(definition("knowledge", true)), "knowledge",
                Collections.<AiEmbeddingStoreAdapter>singletonList(adapter));

        assertSame(adapter.store, registry.get("knowledge"));
        assertSame(adapter.store, registry.getDefault());
        assertEquals("knowledge", registry.getDefaultKey());
        assertTrue(registry.contains("knowledge"));
        assertEquals(Collections.singleton("knowledge"), registry.keys());
    }

    @Test
    public void shouldRejectDuplicateEnabledStoreKeyWithoutLeakingProperties() {
        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultAiEmbeddingStoreRegistry(
                        Arrays.asList(definition("knowledge", true), definition("knowledge", true)),
                        "knowledge", Collections.<AiEmbeddingStoreAdapter>singletonList(new FakeAdapter())));

        assertContext(exception, "knowledge");
        assertNoSecrets(exception);
    }

    @Test
    public void shouldRejectMissingDefaultStoreKey() {
        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultAiEmbeddingStoreRegistry(
                        Collections.singletonList(definition("knowledge", true)), "missing",
                        Collections.<AiEmbeddingStoreAdapter>singletonList(new FakeAdapter())));

        assertContext(exception, "missing");
        assertNoSecrets(exception);
    }

    @Test
    public void shouldRejectDisabledDefaultStoreWithoutLeakingProperties() {
        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultAiEmbeddingStoreRegistry(
                        Collections.singletonList(definition("knowledge", false)), "knowledge",
                        Collections.<AiEmbeddingStoreAdapter>singletonList(new FakeAdapter())));

        assertContext(exception, "knowledge");
        assertNoSecrets(exception);
    }

    @Test
    public void shouldReportDefinitionContextWhenNoAdapterSupportsStore() {
        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultAiEmbeddingStoreFactory().create(definition("knowledge", true),
                        Collections.<AiEmbeddingStoreAdapter>singletonList(new StubAdapter())));

        assertContext(exception, "knowledge");
        assertNoSecrets(exception);
    }

    @Test
    public void shouldWrapSupportsExceptionWithSafeDefinitionContext() {
        IllegalStateException cause = new IllegalStateException("properties={password=top-secret-password}");
        AiEmbeddingStoreAdapter adapter = new StubAdapter() {
            @Override
            public boolean supports(AiEmbeddingStoreDefinition definition) {
                throw cause;
            }
        };

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultAiEmbeddingStoreFactory().create(definition("knowledge", true),
                        Collections.singletonList(adapter)));

        assertContext(exception, "knowledge");
        assertNull(exception.getCause());
        assertNoSecrets(exception);
    }

    @Test
    public void shouldWrapCreateExceptionWithSafeDefinitionContext() {
        IllegalStateException cause = new IllegalStateException("properties={password=top-secret-password}");
        AiEmbeddingStoreAdapter adapter = new StubAdapter() {
            @Override
            public boolean supports(AiEmbeddingStoreDefinition definition) {
                return true;
            }

            @Override
            public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition) {
                throw cause;
            }
        };

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultAiEmbeddingStoreFactory().create(definition("knowledge", true),
                        Collections.singletonList(adapter)));

        assertContext(exception, "knowledge");
        assertNull(exception.getCause());
        assertNoSecrets(exception);
    }

    @Test
    public void shouldRejectNullStoreWithSafeDefinitionContext() {
        AiEmbeddingStoreAdapter adapter = new StubAdapter() {
            @Override
            public boolean supports(AiEmbeddingStoreDefinition definition) {
                return true;
            }
        };

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultAiEmbeddingStoreFactory().create(definition("knowledge", true),
                        Collections.singletonList(adapter)));

        assertContext(exception, "knowledge");
        assertNoSecrets(exception);
    }

    @Test
    public void shouldDefensivelyCopyDefinitionDocumentVectorAndBatchResults() {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("endpoint", "initial");
        AiEmbeddingStoreDefinition definition = definition("knowledge", true);
        definition.setProperties(properties);
        properties.put("endpoint", "changed");
        assertEquals("initial", definition.getProperties().get("endpoint"));
        assertThrows(UnsupportedOperationException.class,
                () -> definition.getProperties().put("extra", "value"));

        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("tenant", "alpha");
        float[] values = new float[]{0.1f, 0.2f};
        AiEmbeddingVector vector = new AiEmbeddingVector(values);
        AiEmbeddingDocument document = new AiEmbeddingDocument("doc-1", "hello", metadata, vector);
        metadata.put("tenant", "beta");
        values[0] = 9.9f;
        assertEquals("alpha", document.getMetadata().get("tenant"));
        assertThrows(UnsupportedOperationException.class,
                () -> document.getMetadata().put("extra", "value"));
        assertArrayEquals(new float[]{0.1f, 0.2f}, vector.toArray());
        float[] exported = vector.toArray();
        exported[0] = 7.7f;
        assertArrayEquals(new float[]{0.1f, 0.2f}, vector.toArray());

        List<AiEmbeddingMatch> matches = new ArrayList<AiEmbeddingMatch>();
        matches.add(new AiEmbeddingMatch(0.9d, "embedding-1", document));
        AiEmbeddingSearchResult result = new AiEmbeddingSearchResult(matches);
        matches.clear();
        assertEquals(1, result.getMatches().size());
        assertThrows(UnsupportedOperationException.class, () -> result.getMatches().clear());
        assertNotSame(matches, result.getMatches());
    }

	@Test
	public void shouldDefensivelyCopyEmbeddingQueryFilters() {
		Map<String, Object> filters = new LinkedHashMap<String, Object>();
		filters.put("projectId", "project-1");
		List<String> tables = new ArrayList<String>(Collections.singletonList("orders"));
		Map<String, Object> authorization = new LinkedHashMap<String, Object>();
		authorization.put("tables", tables);
		filters.put("authorization", authorization);
		AiEmbeddingQuery query = new AiEmbeddingQuery(new AiEmbeddingVector(new float[] { 0.1F, 0.2F }),
				5, 0.6D, filters);

		filters.put("projectId", "project-2");
		tables.add("secrets");

		assertEquals("project-1", query.getFilters().get("projectId"));
		assertEquals(Collections.singletonList("orders"),
				((Map<?, ?>) query.getFilters().get("authorization")).get("tables"));
		assertThrows(UnsupportedOperationException.class,
				() -> query.getFilters().put("operatorId", "operator-1"));
		assertThrows(UnsupportedOperationException.class,
				() -> ((List<Object>) ((Map<?, ?>) query.getFilters().get("authorization")).get("tables"))
						.add("secrets"));
	}

	@Test
	public void shouldRejectInvalidEmbeddingQueryParameters() {
		AiEmbeddingVector vector = new AiEmbeddingVector(new float[] { 0.1F });

		assertThrows(IllegalArgumentException.class, () -> new AiEmbeddingQuery(null, 5, 0.0D));
		assertThrows(IllegalArgumentException.class, () -> new AiEmbeddingQuery(vector, 0, 0.0D));
		assertThrows(IllegalArgumentException.class, () -> new AiEmbeddingQuery(vector, 5, -0.1D));
		assertThrows(IllegalArgumentException.class, () -> new AiEmbeddingQuery(vector, 5, 1.1D));
		assertThrows(IllegalArgumentException.class, () -> new AiEmbeddingQuery(vector, 5, Double.NaN));
	}

	@Test
	public void shouldDefaultToRejectingStoreSideFilters() {
		assertFalse(new FakeStore().supportsFilters());
	}

    private AiEmbeddingStoreDefinition definition(String key, boolean enabled) {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey(key);
        definition.setFramework("fake");
        definition.setProvider("fake-provider");
        definition.setType(AiEmbeddingStoreType.VECTOR);
        definition.setEnabled(enabled);
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("password", "top-secret-password");
        definition.setProperties(properties);
        return definition;
    }

    private void assertContext(AiEmbeddingStoreException exception, String key) {
        assertTrue(exception.getMessage().contains("key=" + key));
        assertTrue(exception.getMessage().contains("framework="));
        assertTrue(exception.getMessage().contains("provider="));
        assertTrue(exception.getMessage().contains("type="));
    }

    private void assertNoSecrets(AiEmbeddingStoreException exception) {
        StringWriter output = new StringWriter();
        exception.printStackTrace(new PrintWriter(output));
        assertFalse(output.toString().contains("top-secret-password"));
        assertFalse(output.toString().contains("properties="));
    }

    private static class FakeAdapter implements AiEmbeddingStoreAdapter {

        private final AiEmbeddingStore store = new FakeStore();

        @Override
        public String getFramework() {
            return "fake";
        }

        @Override
        public boolean supports(AiEmbeddingStoreDefinition definition) {
            return "fake".equals(definition.getFramework())
                    && "fake-provider".equals(definition.getProvider())
                    && AiEmbeddingStoreType.VECTOR == definition.getType();
        }

        @Override
        public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition) {
            return store;
        }
    }

    private static class StubAdapter implements AiEmbeddingStoreAdapter {

        @Override
        public String getFramework() {
            return "stub";
        }

        @Override
        public boolean supports(AiEmbeddingStoreDefinition definition) {
            return false;
        }

        @Override
        public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition) {
            return null;
        }
    }

    private static class FakeStore implements AiEmbeddingStore {

        @Override
        public String getKey() {
            return "knowledge";
        }

        @Override
        public String getFramework() {
            return "fake";
        }

        @Override
        public String getProvider() {
            return "fake-provider";
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
