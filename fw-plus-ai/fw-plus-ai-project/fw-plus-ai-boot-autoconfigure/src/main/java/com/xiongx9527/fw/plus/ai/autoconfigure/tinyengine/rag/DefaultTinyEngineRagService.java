package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultTinyEngineRagService implements TinyEngineRagService {

	private final AiEmbeddingStoreRegistry storeRegistry;
	private final String storeKey;
	private final int maxResults;
	private final double minScore;

	public DefaultTinyEngineRagService(AiEmbeddingStoreRegistry storeRegistry, String storeKey, int maxResults,
			double minScore) {
		this.storeRegistry = storeRegistry;
		this.storeKey = storeKey;
		this.maxResults = maxResults;
		this.minScore = minScore;
	}

	@Override
	public boolean isAvailable() { return true; }

	@Override
	public String getUnavailableReason() { return null; }

	@Override
	public TinyEngineRagSearchResult search(TinyEngineRagRequest request) {
		if (request == null || request.getVector() == null) {
			return new TinyEngineRagSearchResult(Collections.<TinyEngineRagMatch>emptyList());
		}
		AiEmbeddingSearchResult result = resolveStore(request.getStoreKey()).search(new AiEmbeddingQuery(request.getVector(),
				request.getMaxResults() > 0 ? request.getMaxResults() : maxResults,
				request.getMinScore() > 0.0D ? request.getMinScore() : minScore));
		if (result == null || result.getMatches().isEmpty()) {
			return new TinyEngineRagSearchResult(Collections.<TinyEngineRagMatch>emptyList());
		}
		List<TinyEngineRagMatch> matches = new ArrayList<TinyEngineRagMatch>();
		for (AiEmbeddingMatch match : result.getMatches()) {
			AiEmbeddingDocument document = match == null ? null : match.getDocument();
			if (document != null && matchesCollection(document, request.getCollection())) {
				matches.add(new TinyEngineRagMatch(match.getScore(), document.getText(), document.getMetadata()));
			}
		}
		return new TinyEngineRagSearchResult(matches);
	}

	private AiEmbeddingStore resolveStore(String requestedKey) {
		if (storeRegistry == null) {
			throw new IllegalStateException("TinyEngine AI RAG embedding store registry is unavailable");
		}
		String key = isBlank(requestedKey) ? storeKey : requestedKey;
		AiEmbeddingStore store = isBlank(key) ? storeRegistry.getDefault() : storeRegistry.get(key);
		if (store == null) {
			throw new IllegalStateException("TinyEngine AI RAG embedding store is unavailable: "
					+ (isBlank(key) ? "default" : key));
		}
		return store;
	}

	private boolean matchesCollection(AiEmbeddingDocument document, String collection) {
		return isBlank(collection) || collection.equals(String.valueOf(document.getMetadata().get("collection")));
	}

	private boolean isBlank(String value) { return value == null || value.trim().length() == 0; }
}
