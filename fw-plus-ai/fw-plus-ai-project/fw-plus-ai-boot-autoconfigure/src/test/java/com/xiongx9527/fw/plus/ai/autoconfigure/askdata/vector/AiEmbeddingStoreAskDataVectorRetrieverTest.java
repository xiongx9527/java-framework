package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingModel;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingRequest;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingResult;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.model.AiModelUsage;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AiEmbeddingStoreAskDataVectorRetrieverTest {

    @Test
    public void shouldRetrieveDocumentsFromNeutralEmbeddingSearchResult() {
        RecordingStore store = new RecordingStore(new AiEmbeddingSearchResult(Arrays.asList(
                new AiEmbeddingMatch(0.9D, "doc-1", new AiEmbeddingDocument("doc-1", "GMV 是成交金额",
                        Collections.<String, Object>singletonMap("source", "glossary"), null)))));
        AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
                new SingleStoreRegistry("knowledge", store), embeddingModels(), "knowledge");

        List<AskDataRetrievedDocument> documents = retriever.retrieve(new AskDataVectorQuery("term", "orders", "ignored",
                "GMV", 3, Collections.<String, Object>emptyMap()));

        assertEquals(1, documents.size());
        assertEquals("GMV 是成交金额", documents.get(0).getContent());
        assertEquals("glossary", documents.get(0).getMetadata().get("source"));
        assertEquals(3, store.query.getMaxResults());
        assertEquals(2, store.query.getVector().dimension());
    }

    @Test
    public void shouldRouteNeutralEmbeddingModelThroughCoreRouter() {
        RecordingStore store = new RecordingStore(new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList()));
        CapturingEmbeddingModel model = new CapturingEmbeddingModel("embedding-primary");
        RecordingModelRouter router = new RecordingModelRouter();
        AskDataProperties properties = new AskDataProperties();
        properties.getModel().setDefaultEmbeddingModel("embedding-primary");
        properties.getModel().setEmbeddingCandidates(Collections.singletonList("embedding-primary"));
        AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
                new SingleStoreRegistry("knowledge", store), embeddingModels(), singleModelRegistry(model), router,
                properties, "knowledge");
        AskDataProjectContext context = new AskDataProjectContext("project-1", "operator-1");

        retriever.retrieve(new AskDataVectorQuery(context, null, "term", "orders", "knowledge", "GMV", 3,
                Collections.<String, Object>emptyMap()));

        assertEquals(context, router.request.getProjectContext());
        assertEquals("embedding-primary", router.request.getDefaultKey());
        assertEquals(Collections.singletonList("GMV"), model.request.getInputs());
        assertEquals(30000L, model.request.getTimeoutMillis());
        assertEquals(2, store.query.getVector().dimension());
    }

    @Test
    public void shouldFallbackAfterRetryableNeutralEmbeddingInfrastructureFailure() {
        RecordingStore store = new RecordingStore(new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList()));
        CapturingEmbeddingModel primary = new CapturingEmbeddingModel("embedding-primary");
        primary.failure = AiModelException.infrastructureFailure("temporary outage", null);
        CapturingEmbeddingModel fallback = new CapturingEmbeddingModel("embedding-fallback");
        AskDataProperties properties = new AskDataProperties();
        properties.getModel().setEmbeddingCandidates(Collections.singletonList("embedding-primary"));
        properties.getModel().setDefaultEmbeddingModel("embedding-fallback");
        AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
                new SingleStoreRegistry("knowledge", store), embeddingModels(),
                modelRegistry(primary, fallback), new RecordingModelRouter(), properties, "knowledge");

        retriever.retrieve(new AskDataVectorQuery(new AskDataProjectContext("project-1", "operator-1"), null,
                "term", "orders", "knowledge", "GMV", 3, Collections.<String, Object>emptyMap()));

        assertEquals(1, primary.callCount);
        assertEquals(1, fallback.callCount);
        assertEquals(2, store.query.getVector().dimension());
    }

    @Test
    public void shouldReturnStructuredAttemptsAfterNeutralEmbeddingFallback() {
        RecordingStore store = new RecordingStore(new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList()));
        CapturingEmbeddingModel primary = new CapturingEmbeddingModel("embedding-primary");
        primary.failure = AiModelException.infrastructureFailure("temporary outage", null);
        CapturingEmbeddingModel fallback = new CapturingEmbeddingModel("embedding-fallback");
        AskDataProperties properties = new AskDataProperties();
        properties.getModel().setEmbeddingCandidates(Collections.singletonList("embedding-primary"));
        properties.getModel().setDefaultEmbeddingModel("embedding-fallback");
        AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
                new SingleStoreRegistry("knowledge", store), embeddingModels(),
                modelRegistry(primary, fallback), new RecordingModelRouter(), properties, "knowledge");

        AskDataVectorRetrievalResult result = retriever.retrieveWithResult(new AskDataVectorQuery(
                new AskDataProjectContext("project-1", "operator-1"), null, "term", "orders",
                "knowledge", "GMV", 3, Collections.<String, Object>emptyMap()));

        assertEquals("embedding-fallback", result.getActualEmbeddingModelKey());
        assertEquals(4, result.getModelAttempts().size());
        assertEquals(AskDataEvent.ModelState.ATTEMPTED, result.getModelAttempts().get(0).getState());
        assertEquals(AskDataModelRoutePolicy.PROJECT_KEY,
                result.getModelAttempts().get(0).getRoutePolicy());
        assertEquals(AskDataEvent.ModelState.FAILED, result.getModelAttempts().get(1).getState());
        assertEquals(AiModelException.MODEL_INFRASTRUCTURE_FAILURE,
                result.getModelAttempts().get(1).getFailureCode());
        assertEquals("RETRYABLE_INFRASTRUCTURE_FAILURE",
                result.getModelAttempts().get(1).getSwitchReason());
        assertEquals(AskDataEvent.ModelState.ATTEMPTED, result.getModelAttempts().get(2).getState());
        assertEquals(AskDataEvent.ModelState.SUCCEEDED, result.getModelAttempts().get(3).getState());
        assertEquals("embedding-fallback", result.getModelAttempts().get(3).getActualModelKey());
    }

    @Test
    public void shouldNotFallbackAfterNeutralEmbeddingRequestRejection() {
        RecordingStore store = new RecordingStore(new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList()));
        CapturingEmbeddingModel primary = new CapturingEmbeddingModel("embedding-primary");
        primary.failure = AiModelException.requestRejected("invalid request", null);
        CapturingEmbeddingModel fallback = new CapturingEmbeddingModel("embedding-fallback");
        AskDataProperties properties = new AskDataProperties();
        properties.getModel().setEmbeddingCandidates(Collections.singletonList("embedding-primary"));
        properties.getModel().setDefaultEmbeddingModel("embedding-fallback");
        AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
                new SingleStoreRegistry("knowledge", store), embeddingModels(),
                modelRegistry(primary, fallback), new RecordingModelRouter(), properties, "knowledge");

        AiModelException exception = assertThrows(AiModelException.class,
                () -> retriever.retrieve(new AskDataVectorQuery(
                        new AskDataProjectContext("project-1", "operator-1"), null, "term", "orders",
                        "knowledge", "GMV", 3, Collections.<String, Object>emptyMap())));

        assertEquals(AiModelException.MODEL_REQUEST_REJECTED, exception.getErrorCode());
        assertEquals(1, primary.callCount);
        assertEquals(0, fallback.callCount);
        assertEquals(0, store.searchCount);
    }

    @Test
    public void shouldPreserveStructuredAttemptWhenNeutralEmbeddingFails() {
        RecordingStore store = new RecordingStore(new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList()));
        CapturingEmbeddingModel primary = new CapturingEmbeddingModel("embedding-primary");
        primary.failure = AiModelException.requestRejected("invalid request", null);
        AskDataProperties properties = new AskDataProperties();
        properties.getModel().setDefaultEmbeddingModel("embedding-primary");
        AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
                new SingleStoreRegistry("knowledge", store), embeddingModels(), singleModelRegistry(primary),
                new RecordingModelRouter(), properties, "knowledge");

        AskDataVectorRetrievalException exception = assertThrows(AskDataVectorRetrievalException.class,
                () -> retriever.retrieveWithResult(new AskDataVectorQuery(
                        new AskDataProjectContext("project-1", "operator-1"), null, "term", "orders",
                        "knowledge", "GMV", 3, Collections.<String, Object>emptyMap())));

        assertEquals(AiModelException.MODEL_REQUEST_REJECTED, exception.getErrorCode());
        assertEquals(2, exception.getModelAttempts().size());
        assertEquals(AskDataEvent.ModelState.ATTEMPTED, exception.getModelAttempts().get(0).getState());
        assertEquals(AskDataEvent.ModelState.FAILED, exception.getModelAttempts().get(1).getState());
        assertEquals("NON_RETRYABLE_FAILURE", exception.getModelAttempts().get(1).getSwitchReason());
    }

    @Test
    public void shouldReturnEmptyListForBlankQuery() {
        RecordingStore store = new RecordingStore(new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList()));
        AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
                new SingleStoreRegistry("knowledge", store), embeddingModels(), "knowledge");

        assertTrue(retriever.retrieve(new AskDataVectorQuery("term", "orders", "knowledge", " ", 3,
                Collections.<String, Object>emptyMap())).isEmpty());
        assertEquals(0, store.searchCount);
    }

	@Test
	public void shouldPreserveAuthorizedFiltersWhenStoreSupportsThem() {
		RecordingStore store = new RecordingStore(
				new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList()), true);
		AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
				new SingleStoreRegistry("knowledge", store), embeddingModels(), "knowledge");
		Map<String, Object> filters = new LinkedHashMap<String, Object>();
		filters.put("projectId", "project-1");

		retriever.retrieve(new AskDataVectorQuery("term", "orders", "knowledge", "GMV", 3, filters));
		filters.put("projectId", "project-2");

		assertEquals("project-1", store.query.getFilters().get("projectId"));
		assertThrows(UnsupportedOperationException.class,
				() -> store.query.getFilters().put("operatorId", "operator-1"));
	}

	@Test
	public void shouldRejectAuthorizedFiltersWhenStoreCannotApplyThem() {
		RecordingStore store = new RecordingStore(
				new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList()));
		AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
				new SingleStoreRegistry("knowledge", store), embeddingModels(), "knowledge");

		AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
				() -> retriever.retrieve(new AskDataVectorQuery("term", "orders", "knowledge", "GMV", 3,
						Collections.<String, Object>singletonMap("projectId", "project-1"))));

		assertTrue(exception.getMessage().contains("knowledge"));
		assertEquals(0, store.searchCount);
	}

    @Test
    public void shouldDiagnoseMissingConfiguredStore() {
        AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(
                new SingleStoreRegistry(null, null), embeddingModels(), "missing");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> retriever.retrieve(new AskDataVectorQuery("term", "orders", "knowledge", "GMV", 3,
                        Collections.<String, Object>emptyMap())));

        assertTrue(exception.getMessage().contains("missing"));
        assertTrue(exception.getMessage().contains("向量存储"));
    }

    @Test
    public void shouldDiagnoseMissingDefaultStoreRegistry() {
        AskDataVectorRetriever retriever = new AiEmbeddingStoreAskDataVectorRetriever(null, embeddingModels(), null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> retriever.retrieve(new AskDataVectorQuery("term", "orders", "knowledge", "GMV", 3,
                        Collections.<String, Object>emptyMap())));

        assertTrue(exception.getMessage().contains("默认向量存储"));
    }

    private AskDataModelRegistry embeddingModels() {
        return new AskDataModelRegistry() {
            @Override
            public AskDataChatModel getChatModel(String modelKey) {
                return null;
            }

            @Override
            public AskDataEmbeddingModel getEmbeddingModel(String modelKey) {
                return text -> Arrays.asList(0.1F, 0.2F);
            }

            @Override
            public String getDefaultChatModelKey() {
                return "chat";
            }

            @Override
            public String getDefaultEmbeddingModelKey() {
                return "embedding";
            }
        };
    }

    private AiModelRegistry singleModelRegistry(final AiModel model) {
        return modelRegistry(model);
    }

    private AiModelRegistry modelRegistry(final AiModel... models) {
        return new AiModelRegistry() {
            @Override public AiModel get(String key) {
                for (AiModel model : models) {
                    if (model.getKey().equals(key)) {
                        return model;
                    }
                }
                return null;
            }
            @Override public AiModel getDefault() { return models.length == 0 ? null : models[0]; }
            @Override public String getDefaultKey() { return models.length == 0 ? null : models[0].getKey(); }
            @Override public boolean contains(String key) { return get(key) != null; }
            @Override public Set<String> keys() {
                Set<String> keys = new java.util.LinkedHashSet<String>();
                for (AiModel model : models) {
                    keys.add(model.getKey());
                }
                return Collections.unmodifiableSet(keys);
            }
        };
    }

    private static final class RecordingModelRouter implements AskDataModelRouter {
        private AskDataModelRequest request;

        @Override
        public AskDataModelResult route(AskDataModelRequest request) {
            this.request = request;
            return AskDataModelRouter.super.route(request);
        }
    }

    private static final class CapturingEmbeddingModel implements AiEmbeddingModel {
        private final String key;
        private AiEmbeddingRequest request;
        private AiModelException failure;
        private int callCount;

        private CapturingEmbeddingModel(String key) { this.key = key; }

        @Override
        public AiEmbeddingResult embed(AiEmbeddingRequest request) {
            this.request = request;
            callCount++;
            if (failure != null) {
                throw failure;
            }
            return new AiEmbeddingResult(Collections.singletonList(new float[] { 0.1F, 0.2F }), key,
                    AiModelUsage.unknown());
        }

        @Override public String getKey() { return key; }
        @Override public String getFramework() { return "test"; }
        @Override public String getProvider() { return "test"; }
        @Override public AiModelType getType() { return AiModelType.EMBEDDING; }
    }

    private static final class RecordingStore implements AiEmbeddingStore {

        private final AiEmbeddingSearchResult result;
		private final boolean supportsFilters;
        private int searchCount;
        private AiEmbeddingQuery query;

        private RecordingStore(AiEmbeddingSearchResult result) {
			this(result, false);
		}

		private RecordingStore(AiEmbeddingSearchResult result, boolean supportsFilters) {
            this.result = result;
			this.supportsFilters = supportsFilters;
        }

        @Override public String getKey() { return "knowledge"; }
        @Override public String getFramework() { return "test"; }
        @Override public String getProvider() { return "test"; }
        @Override public AiEmbeddingStoreType getType() { return AiEmbeddingStoreType.VECTOR; }
        @Override public String add(AiEmbeddingDocument document) { return null; }
        @Override public List<String> addAll(List<AiEmbeddingDocument> documents) { return Collections.emptyList(); }
        @Override public void remove(String id) { }
        @Override public void removeAll(List<String> ids) { }
		@Override public boolean supportsFilters() { return supportsFilters; }

        @Override
        public AiEmbeddingSearchResult search(AiEmbeddingQuery query) {
            searchCount++;
            this.query = query;
            return result;
        }
    }

    private static final class SingleStoreRegistry implements AiEmbeddingStoreRegistry {

        private final String key;
        private final AiEmbeddingStore store;

        private SingleStoreRegistry(String key, AiEmbeddingStore store) {
            this.key = key;
            this.store = store;
        }

        @Override public AiEmbeddingStore get(String storeKey) { return key != null && key.equals(storeKey) ? store : null; }
        @Override public AiEmbeddingStore getDefault() { return store; }
        @Override public String getDefaultKey() { return key; }
        @Override public boolean contains(String storeKey) { return key != null && key.equals(storeKey); }
        @Override public Set<String> keys() { return key == null ? Collections.<String>emptySet() : Collections.singleton(key); }
    }
}
