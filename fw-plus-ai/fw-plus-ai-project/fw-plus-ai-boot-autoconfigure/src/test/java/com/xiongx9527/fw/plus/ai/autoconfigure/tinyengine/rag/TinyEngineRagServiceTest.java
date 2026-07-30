package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TinyEngineRagServiceTest {

	@Test
	public void shouldMapScoreContentAndMetadataAndFilterCollection() {
		Map<String, Object> matchedMetadata = new LinkedHashMap<String, Object>();
		matchedMetadata.put("collection", "tiny");
		matchedMetadata.put("source", "guide.txt");
		Map<String, Object> otherMetadata = new LinkedHashMap<String, Object>();
		otherMetadata.put("collection", "other");
		AiEmbeddingStore store = new SearchStore(Arrays.asList(
				new AiEmbeddingMatch(0.93D, "matched", new AiEmbeddingDocument("matched", "matched content", matchedMetadata, null)),
				new AiEmbeddingMatch(0.87D, "other", new AiEmbeddingDocument("other", "other content", otherMetadata, null))));
		TinyEngineRagRequest request = new TinyEngineRagRequest();
		request.setVector(new AiEmbeddingVector(new float[] { 1.0F }));
		request.setCollection("tiny");

		TinyEngineRagSearchResult result = new DefaultTinyEngineRagService(new Registry(store), "knowledge", 5, 0.0D).search(request);

		assertEquals(1, result.getMatches().size());
		assertEquals(0.93D, result.getMatches().get(0).getScore());
		assertEquals("matched content", result.getMatches().get(0).getContent());
		assertEquals("guide.txt", result.getMatches().get(0).getMetadata().get("source"));
	}

	@Test
	public void shouldFailFastWhenConfiguredStoreIsMissing() {
		TinyEngineRagRequest request = new TinyEngineRagRequest();
		request.setVector(new AiEmbeddingVector(new float[] { 1.0F }));
		assertThrows(IllegalStateException.class,
				() -> new DefaultTinyEngineRagService(new Registry(null), "missing", 5, 0.0D).search(request));
	}

	private static final class Registry implements AiEmbeddingStoreRegistry {
		private final AiEmbeddingStore store;
		private Registry(AiEmbeddingStore store) { this.store = store; }
		@Override public AiEmbeddingStore get(String key) { return "knowledge".equals(key) && store != null ? store : null; }
		@Override public AiEmbeddingStore getDefault() { return store; }
		@Override public String getDefaultKey() { return "knowledge"; }
		@Override public boolean contains(String key) { return "knowledge".equals(key) && store != null; }
		@Override public Set<String> keys() { return Collections.singleton("knowledge"); }
	}

	private static final class SearchStore implements AiEmbeddingStore {
		private final List<AiEmbeddingMatch> matches;
		private SearchStore(List<AiEmbeddingMatch> matches) { this.matches = matches; }
		@Override public String getKey() { return "knowledge"; }
		@Override public String getFramework() { return "test"; }
		@Override public String getProvider() { return "test"; }
		@Override public AiEmbeddingStoreType getType() { return AiEmbeddingStoreType.VECTOR; }
		@Override public String add(AiEmbeddingDocument document) { return document.getId(); }
		@Override public List<String> addAll(List<AiEmbeddingDocument> documents) { return Collections.emptyList(); }
		@Override public void remove(String id) { }
		@Override public void removeAll(List<String> ids) { }
		@Override public AiEmbeddingSearchResult search(AiEmbeddingQuery query) { return new AiEmbeddingSearchResult(matches); }
	}
}
