package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowMetadataCondition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowReranker;
import com.xiongx9527.fw.plus.ai.tinyflow.local.FwPlusLocalKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TinyflowLocalRetrieverTest {

	@Test
	public void shouldRetrieveVectorKeywordAndHybridModesWithCompleteSources() {
		Fixture fixture = new Fixture();
		for (String mode : Arrays.asList("VECTOR", "KEYWORD", "HYBRID")) {
			TinyflowKnowledgeResult result = fixture.retriever.retrieve(fixture.request(mode, 5, null));

			assertFalse(mode, result.getMatches().isEmpty());
			assertEveryMatchHasSourceIds(result);
			for (TinyflowKnowledgeMatch match : result.getMatches()) {
				assertEquals("tenant-a", match.getTenantId());
				assertEquals("knowledge-a", match.getKnowledgeId());
				assertEquals("original-value", match.getMetadata().get("custom"));
			}
		}
	}

	@Test
	public void shouldBoundCandidatesDeduplicateAndFilterDisabledResources() {
		Fixture fixture = new Fixture();
		TinyflowKnowledgeResult result = fixture.retriever.retrieve(fixture.request("HYBRID", 50, null));

		assertEquals(3, result.getMatches().size());
		assertEquals(3, distinctChunkIds(result));
		assertFalse(containsChunk(result, "disabled-chunk"));
		assertFalse(containsChunk(result, "disabled-document-chunk"));
		assertFalse(containsChunk(result, "inactive-document-chunk"));
	}

	@Test
	public void shouldApplyMetadataConditionsAfterBoundedRecall() {
		Fixture fixture = new Fixture();
		TinyflowMetadataCondition condition = TinyflowMetadataCondition.builder()
				.field("custom").operator("is").value("does-not-match").build();

		TinyflowKnowledgeResult result = fixture.retriever.retrieve(
				fixture.request("HYBRID", 5, null, Collections.singletonList(condition), "and"));

		assertTrue(result.getMatches().isEmpty());
	}

	@Test
	public void shouldNormalizeThenApplyWeightedFusionAndDeduplicate() {
		Fixture fixture = new Fixture();
		TinyflowKnowledgeMatch chunkA = TinyflowKeywordRetriever.match(fixture.findChunk("chunk-b"), 0.9D);
		TinyflowKnowledgeMatch chunkB = TinyflowKeywordRetriever.match(fixture.findChunk("chunk-c"), 0.7D);
		TinyflowHybridFusion fusion = new TinyflowHybridFusion();

		List<TinyflowKnowledgeMatch> result = fusion.fuse(
				Arrays.asList(chunkA, chunkB, TinyflowKeywordRetriever.match(fixture.findChunk("chunk-b"), 0.8D)),
				Arrays.asList(TinyflowKeywordRetriever.match(fixture.findChunk("chunk-c"), 0.8D),
						TinyflowKeywordRetriever.match(fixture.findChunk("chunk-b"), 0.2D)),
				"WEIGHTED", 0.75D, 0.25D);

		assertEquals(2, result.size());
		assertEquals("chunk-b", result.get(0).getChunkId());
		assertEquals(0.75D, result.get(0).getScore(), 0.00001D);
		assertEquals("chunk-c", result.get(1).getChunkId());
		assertEquals(0.25D, result.get(1).getScore(), 0.00001D);
	}

	@Test
	public void shouldFuseRrfUsingOneBasedRanksAndConstantSixty() {
		Fixture fixture = new Fixture();
		TinyflowKnowledgeMatch chunkA = TinyflowKeywordRetriever.match(fixture.findChunk("chunk-b"), 0.9D);
		TinyflowKnowledgeMatch chunkB = TinyflowKeywordRetriever.match(fixture.findChunk("chunk-c"), 0.7D);

		List<TinyflowKnowledgeMatch> result = new TinyflowHybridFusion().fuse(
				Arrays.asList(chunkA, chunkB),
				Arrays.asList(TinyflowKeywordRetriever.match(fixture.findChunk("chunk-b"), 0.8D),
						TinyflowKeywordRetriever.match(fixture.findChunk("chunk-c"), 0.2D)),
				"RRF", 0.5D, 0.5D);

		assertEquals(2, result.size());
		assertEquals("chunk-b", result.get(0).getChunkId());
		assertEquals(0.01639344262295082D, result.get(0).getScore(), 0.000000000000001D);
		assertEquals("chunk-c", result.get(1).getChunkId());
		assertEquals(0.016129032258064516D, result.get(1).getScore(), 0.000000000000001D);
	}

	@Test
	public void shouldUseDefaultCandidateMultiplierFive() {
		Fixture fixture = new Fixture();
		TinyflowLocalRetriever retriever = new TinyflowLocalRetriever(fixture.repository, new ModelProvider(),
				new StoreProvider(new CandidateSensitiveVectorStore()), null);

		TinyflowKnowledgeResult result = retriever.retrieve(fixture.requestWithDefaultCandidates("VECTOR", 3));

		assertEquals(1, result.getMatches().size());
		assertEquals("chunk-b", result.getMatches().get(0).getChunkId());
	}

	@Test
	public void shouldNotExpandParentOutsideTenantBoundary() {
		Fixture fixture = new Fixture();

		TinyflowKnowledgeResult result = fixture.retriever.retrieve(fixture.request("HYBRID", 10, null));

		assertFalse(containsChunk(result, "child-foreign-parent"));
		assertFalse(containsChunk(result, "foreign-parent"));
	}

	@Test
	public void shouldTruncateVectorStoreThatReturnsMoreThanHardLimit() {
		Fixture fixture = new Fixture();
		TinyflowLocalRetriever retriever = new TinyflowLocalRetriever(fixture.repository, new ModelProvider(),
				new StoreProvider(new OverflowVectorStore()), null);

		TinyflowKnowledgeResult result = retriever.retrieve(fixture.request("VECTOR", 50, null));

		assertTrue(containsChunk(result, "chunk-b"));
		assertFalse(containsChunk(result, "chunk-c"));
	}

	@Test
	public void shouldTruncateKeywordRepositoryThatIgnoresLimit() {
		Fixture fixture = new Fixture();
		fixture.returnKeywordOverflow = true;

		TinyflowKnowledgeResult result = fixture.retriever.retrieve(
				fixture.requestWithDefaultCandidates("KEYWORD", 3));

		assertFalse(result.getMatches().isEmpty());
		assertFalse(containsChunk(result, "chunk-c"));
	}

	@Test
	public void shouldApplyRerankThenParentExpansionThenThresholdAndTopK() {
		Fixture fixture = new Fixture();
		fixture.reranker = new TinyflowReranker() {
			@Override
			public List<TinyflowKnowledgeMatch> rerank(TinyflowKnowledgeRequest request,
					List<TinyflowKnowledgeMatch> matches) {
				List<TinyflowKnowledgeMatch> result = new ArrayList<TinyflowKnowledgeMatch>();
				for (TinyflowKnowledgeMatch match : matches) {
					double score = "child-a".equals(match.getChunkId()) ? 0.95D
							: ("chunk-b".equals(match.getChunkId()) ? 0.85D : 0.75D);
					result.add(copy(match, score));
				}
				return result;
			}
		};
		fixture.rebuildRetriever();

		TinyflowKnowledgeResult result = fixture.retriever.retrieve(fixture.request("HYBRID", 3, 0.7D));

		assertEquals(3, result.getMatches().size());
		assertEquals("parent-a", result.getMatches().get(0).getChunkId());
		assertEquals(0.95D, result.getMatches().get(0).getScore(), 0.00001D);
		assertTrue(result.getMatches().get(0).getScore() >= result.getMatches().get(1).getScore());
		assertEveryMatchHasSourceIds(result);
	}

	@Test
	public void shouldReturnNotFoundForMissingOrDisabledKnowledgeAndProviderShouldDelegate() {
		Fixture fixture = new Fixture();
		FwPlusLocalKnowledgeProvider provider = new FwPlusLocalKnowledgeProvider(
				fixture.repository, fixture.retriever);

		assertTrue(provider.retrieve(fixture.request("VECTOR", 2, null)).isKnowledgeExists());
		fixture.knowledge = null;
		assertFalse(provider.retrieve(fixture.request("VECTOR", 2, null)).isKnowledgeExists());
		fixture.knowledge = fixture.knowledge(false);
		assertTrue(provider.retrieve(fixture.request("VECTOR", 2, null)).getMatches().isEmpty());
	}

	private static TinyflowKnowledgeMatch copy(TinyflowKnowledgeMatch match, double score) {
		TinyflowChunk chunk = match.getChunk();
		return TinyflowKnowledgeMatch.builder().tenantId(chunk.getTenantId()).knowledgeId(chunk.getKnowledgeId())
				.sourceId(chunk.getSourceId()).sourceType(chunk.getSourceType()).documentId(chunk.getDocumentId())
				.chunkId(chunk.getChunkId()).content(chunk.getContent()).sourceUri(chunk.getSourceUri())
				.sourceVersion(chunk.getSourceVersion()).location(chunk.getLocation()).position(chunk.getPosition())
				.parentChunkId(chunk.getParentChunkId()).tokenCount(chunk.getTokenCount()).metadata(chunk.getMetadata())
				.enabled(chunk.isEnabled()).vectorIds(chunk.getVectorIds()).score(score).build();
	}

	private static void assertEveryMatchHasSourceIds(TinyflowKnowledgeResult result) {
		for (TinyflowKnowledgeMatch match : result.getMatches()) {
			assertNotNull(match.getSourceId());
			assertNotNull(match.getSourceType());
			assertNotNull(match.getDocumentId());
			assertNotNull(match.getChunkId());
			assertNotNull(match.getLocation());
			assertNotNull(match.getMetadata());
		}
	}

	private static int distinctChunkIds(TinyflowKnowledgeResult result) {
		Map<String, Boolean> ids = new LinkedHashMap<String, Boolean>();
		for (TinyflowKnowledgeMatch match : result.getMatches()) {
			ids.put(match.getChunkId(), Boolean.TRUE);
		}
		return ids.size();
	}

	private static boolean containsChunk(TinyflowKnowledgeResult result, String chunkId) {
		for (TinyflowKnowledgeMatch match : result.getMatches()) {
			if (chunkId.equals(match.getChunkId())) {
				return true;
			}
		}
		return false;
	}

	private static final class Fixture implements InvocationHandler {
		private final List<TinyflowChunk> chunks = new ArrayList<TinyflowChunk>();
		private final Map<String, TinyflowDocument> activeDocuments = new LinkedHashMap<String, TinyflowDocument>();
		private TinyflowKnowledgeDefinition knowledge;
		private final TinyflowChunk foreignParent;
		private final TinyflowKnowledgeRepository repository;
		private final BoundedVectorStore vectorStore = new BoundedVectorStore();
		private TinyflowReranker reranker;
		private TinyflowLocalRetriever retriever;
		private boolean returnKeywordOverflow;

		private Fixture() {
			knowledge = knowledge(true);
			foreignParent = TinyflowChunk.builder().tenantId("tenant-b").knowledgeId("knowledge-a")
					.sourceId("source-foreign").sourceType("TEXT").documentId("document-foreign")
					.chunkId("foreign-parent").content("foreign content").location("foreign")
					.vectorIds(Collections.singletonList("vector-foreign")).build();
			chunks.add(chunk("parent-a", "document-a", null, true, "TinyFlow parent overview", "vector-parent"));
			chunks.add(chunk("child-a", "document-a", "parent-a", true, "TinyFlow hybrid retrieval", "vector-a"));
			chunks.add(chunk("chunk-b", "document-b", null, true, "TinyFlow keyword guide", "vector-b"));
			chunks.add(chunk("chunk-c", "document-c", null, true, "TinyFlow metadata filters", "vector-c"));
			chunks.add(chunk("disabled-chunk", "document-d", null, false, "TinyFlow disabled", "vector-disabled"));
			chunks.add(chunk("disabled-document-chunk", "document-disabled", null, true,
					"TinyFlow disabled document", "vector-disabled-document"));
			chunks.add(chunk("child-foreign-parent", "document-boundary", "foreign-parent", true,
					"TinyFlow boundary", "vector-boundary"));
			chunks.add(chunk("inactive-document-chunk", "document-old", null, true, "TinyFlow old", "vector-old"));
			for (TinyflowChunk chunk : chunks) {
				if (!"inactive-document-chunk".equals(chunk.getChunkId())) {
					activeDocuments.put(chunk.getSourceId(), document(chunk,
							!"disabled-document-chunk".equals(chunk.getChunkId())));
				}
			}
			activeDocuments.put("source-inactive-document-chunk",
					document(chunk("replacement", "document-new", null, true, "new", "vector-new")));
			repository = (TinyflowKnowledgeRepository) Proxy.newProxyInstance(
					TinyflowKnowledgeRepository.class.getClassLoader(),
					new Class<?>[] { TinyflowKnowledgeRepository.class }, this);
			rebuildRetriever();
		}

		private void rebuildRetriever() {
			retriever = new TinyflowLocalRetriever(repository, new ModelProvider(),
					new StoreProvider(vectorStore), reranker);
		}

		private TinyflowKnowledgeRequest request(String mode, int topK, Double threshold) {
			return request(mode, topK, threshold, Collections.<TinyflowMetadataCondition>emptyList(), "and");
		}

		private TinyflowKnowledgeRequest request(String mode, int topK, Double threshold,
				List<TinyflowMetadataCondition> conditions, String logic) {
			Map<String, Object> options = new LinkedHashMap<String, Object>();
			options.put("retrievalMode", mode);
			options.put("candidateMultiplier", Integer.valueOf(5));
			options.put("maxCandidateCount", Integer.valueOf(200));
			options.put("fusionMode", "RRF");
			options.put("metadataLogic", logic);
			return TinyflowKnowledgeRequest.builder().providerKey("fw-plus-local")
					.tenantId("tenant-a").knowledgeId("knowledge-a").query("TinyFlow")
					.topK(topK).threshold(threshold).metadataConditions(conditions).metadata(options).build();
		}

		private TinyflowKnowledgeRequest requestWithDefaultCandidates(String mode, int topK) {
			Map<String, Object> options = new LinkedHashMap<String, Object>();
			options.put("retrievalMode", mode);
			return TinyflowKnowledgeRequest.builder().providerKey("fw-plus-local")
					.tenantId("tenant-a").knowledgeId("knowledge-a").query("TinyFlow")
					.topK(topK).metadata(options).build();
		}

		private TinyflowKnowledgeDefinition knowledge(boolean enabled) {
			Map<String, Object> metadata = new LinkedHashMap<String, Object>();
			metadata.put("embeddingModelKey", "embedding-a");
			metadata.put("vectorStoreKey", "store-a");
			return TinyflowKnowledgeDefinition.builder().providerKey("fw-plus-local")
					.tenantId("tenant-a").knowledgeId("knowledge-a").name("Knowledge A")
					.metadata(metadata).enabled(enabled).build();
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			String name = method.getName();
			if ("findKnowledge".equals(name)) {
				return "tenant-a".equals(args[0]) && "knowledge-a".equals(args[1]) ? knowledge : null;
			}
			if ("findChunks".equals(name)) {
				if (returnKeywordOverflow) {
					List<TinyflowChunk> overflow = new ArrayList<TinyflowChunk>();
					for (int index = 0; index < ((Integer) args[2]).intValue(); index++) {
						overflow.add(findChunk("chunk-b"));
					}
					overflow.add(findChunk("chunk-c"));
					return overflow;
				}
				return new ArrayList<TinyflowChunk>(chunks.subList(0,
						Math.min(chunks.size(), ((Integer) args[2]).intValue())));
			}
			if ("findChunksByVectorIds".equals(name)) {
				List<?> ids = (List<?>) args[2];
				List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
				for (TinyflowChunk chunk : chunks) {
					for (String id : chunk.getVectorIds()) {
						if (ids.contains(id)) {
							result.add(chunk);
						}
					}
				}
				return result;
			}
			if ("findChunk".equals(name)) {
				if (foreignParent.getChunkId().equals(args[2])) {
					return foreignParent;
				}
				for (TinyflowChunk chunk : chunks) {
					if (chunk.getChunkId().equals(args[2])) {
						return chunk;
					}
				}
				return null;
			}
			if ("findActiveDocument".equals(name)) {
				return activeDocuments.get(args[2]);
			}
			Class<?> type = method.getReturnType();
			if (List.class.equals(type)) {
				return Collections.emptyList();
			}
			if (long.class.equals(type)) {
				return Long.valueOf(0L);
			}
			return null;
		}

		private TinyflowChunk chunk(String chunkId, String documentId, String parentChunkId,
				boolean enabled, String content, String vectorId) {
			Map<String, Object> metadata = new LinkedHashMap<String, Object>();
			metadata.put("custom", "original-value");
			return TinyflowChunk.builder().tenantId("tenant-a").knowledgeId("knowledge-a")
					.sourceId("source-" + chunkId).sourceType("TEXT").documentId(documentId)
					.chunkId(chunkId).content(content).sourceUri("memory://" + chunkId)
					.sourceVersion("v1").location("paragraph:" + chunkId).position(Integer.valueOf(1))
					.parentChunkId(parentChunkId).tokenCount(Integer.valueOf(3)).metadata(metadata)
					.enabled(enabled).vectorIds(Collections.singletonList(vectorId)).build();
		}

		private TinyflowDocument document(TinyflowChunk chunk) {
			return document(chunk, true);
		}

		private TinyflowDocument document(TinyflowChunk chunk, boolean enabled) {
			return TinyflowDocument.builder().tenantId(chunk.getTenantId()).knowledgeId(chunk.getKnowledgeId())
					.sourceId(chunk.getSourceId()).sourceType(chunk.getSourceType()).documentId(chunk.getDocumentId())
					.checksum("checksum-" + chunk.getDocumentId()).name(chunk.getDocumentId())
					.content(chunk.getContent()).sourceUri(chunk.getSourceUri()).sourceVersion(chunk.getSourceVersion())
					.metadata(chunk.getMetadata()).chunks(Collections.singletonList(chunk)).enabled(enabled).build();
		}

		private TinyflowChunk findChunk(String chunkId) {
			for (TinyflowChunk chunk : chunks) {
				if (chunkId.equals(chunk.getChunkId())) {
					return chunk;
				}
			}
			throw new IllegalArgumentException("Unknown chunk: " + chunkId);
		}
	}

	private static final class BoundedVectorStore implements TinyflowVectorStore {
		@Override
		public List<String> addAll(List<com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorDocument> documents) {
			return Collections.emptyList();
		}

		@Override
		public void removeAll(List<String> ids) {
		}

		@Override
		public List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore) {
			if (maxResults > 200) {
				throw new IllegalStateException("unbounded candidate request");
			}
			Map<String, Object> metadata = Collections.emptyMap();
			return Arrays.asList(
					new TinyflowVectorMatch("vector-disabled", "disabled", 0.99D, metadata),
					new TinyflowVectorMatch("vector-a", "a", 0.90D, metadata),
					new TinyflowVectorMatch("vector-a", "a duplicate", 0.88D, metadata),
					new TinyflowVectorMatch("vector-b", "b", 0.80D, metadata),
					new TinyflowVectorMatch("vector-c", "c", 0.70D, metadata),
					new TinyflowVectorMatch("vector-disabled-document", "disabled document", 0.98D, metadata),
					new TinyflowVectorMatch("vector-boundary", "boundary", 0.89D, metadata),
					new TinyflowVectorMatch("vector-old", "old", 0.95D, metadata));
		}
	}

	private static final class CandidateSensitiveVectorStore implements TinyflowVectorStore {
		@Override
		public List<String> addAll(List<com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorDocument> documents) {
			return Collections.emptyList();
		}

		@Override
		public void removeAll(List<String> ids) {
		}

		@Override
		public List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore) {
			return maxResults == 15
					? Collections.singletonList(new TinyflowVectorMatch(
							"vector-b", "b", 0.8D, Collections.<String, Object>emptyMap()))
					: Collections.<TinyflowVectorMatch>emptyList();
		}
	}

	private static final class OverflowVectorStore implements TinyflowVectorStore {
		@Override
		public List<String> addAll(List<com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorDocument> documents) {
			return Collections.emptyList();
		}

		@Override
		public void removeAll(List<String> ids) {
		}

		@Override
		public List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore) {
			List<TinyflowVectorMatch> matches = new ArrayList<TinyflowVectorMatch>();
			matches.add(new TinyflowVectorMatch(
					"vector-b", "b", 0.8D, Collections.<String, Object>emptyMap()));
			for (int index = 1; index < 200; index++) {
				matches.add(new TinyflowVectorMatch(
						"overflow-" + index, "overflow", 0.1D, Collections.<String, Object>emptyMap()));
			}
			matches.add(new TinyflowVectorMatch(
					"vector-c", "c", 0.99D, Collections.<String, Object>emptyMap()));
			return matches;
		}
	}

	private static final class StoreProvider implements TinyflowVectorStoreProvider {
		private final TinyflowVectorStore store;

		private StoreProvider(TinyflowVectorStore store) {
			this.store = store;
		}

		@Override
		public List<TinyflowVectorStoreDescriptor> list() {
			return Collections.singletonList(new TinyflowVectorStoreDescriptor(
					"store-a", "Store A", "memory", "test", false));
		}

		@Override
		public TinyflowVectorStore get(String key) {
			return "store-a".equals(key) ? store : null;
		}
	}

	private static final class ModelProvider implements TinyflowModelProvider {
		@Override public List<TinyflowModelDescriptor> listChatModels() { return Collections.emptyList(); }
		@Override public List<TinyflowModelDescriptor> listEmbeddingModels() { return Collections.emptyList(); }
		@Override public ChatLanguageModel getChatModel(String key) { return null; }
		@Override public TinyflowEmbeddingModel getEmbeddingModel(String key) {
			return "embedding-a".equals(key) ? text -> new float[] { 1F, 0F } : null;
		}
	}
}
