package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegistryTinyflowVectorStoreProvider implements TinyflowVectorStoreProvider {

	private final AiEmbeddingStoreRegistry storeRegistry;

	public RegistryTinyflowVectorStoreProvider(AiEmbeddingStoreRegistry storeRegistry) {
		this.storeRegistry = storeRegistry;
	}

	@Override
	public List<TinyflowVectorStoreDescriptor> list() {
		if (storeRegistry == null) {
			return Collections.emptyList();
		}
		List<TinyflowVectorStoreDescriptor> descriptors = new ArrayList<TinyflowVectorStoreDescriptor>();
		for (String key : storeRegistry.keys()) {
			if (!storeRegistry.contains(key)) {
				continue;
			}
			AiEmbeddingStore store = storeRegistry.get(key);
			if (store != null) {
				descriptors.add(new TinyflowVectorStoreDescriptor(key, key, store.getType().name(),
						store.getProvider(), false));
			}
		}
		return descriptors;
	}

	@Override
	public TinyflowVectorStore get(String key) {
		final String resolvedKey = resolveKey(key);
		if (storeRegistry == null || !storeRegistry.contains(resolvedKey)) {
			throw new TinyflowException("Tinyflow vector store not found: " + resolvedKey);
		}
		final AiEmbeddingStore store = storeRegistry.get(resolvedKey);
		if (store == null) {
			throw new TinyflowException("Tinyflow vector store not found: " + resolvedKey);
		}
		return new TinyflowVectorStore() {
			@Override
			public List<String> addAll(List<TinyflowVectorDocument> documents) {
				return store.addAll(toDocuments(documents));
			}

			@Override
			public void removeAll(List<String> ids) {
				store.removeAll(ids);
			}

			@Override
			public List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore) {
				AiEmbeddingSearchResult result = store.search(new AiEmbeddingQuery(new AiEmbeddingVector(vector),
						maxResults, minScore));
				return toMatches(result);
			}
		};
	}

	private List<AiEmbeddingDocument> toDocuments(List<TinyflowVectorDocument> documents) {
		if (documents == null || documents.isEmpty()) {
			return Collections.emptyList();
		}
		List<AiEmbeddingDocument> converted = new ArrayList<AiEmbeddingDocument>();
		for (TinyflowVectorDocument document : documents) {
			converted.add(new AiEmbeddingDocument(document.getId(), document.getContent(), document.getMetadata(),
					new AiEmbeddingVector(document.getVector())));
		}
		return converted;
	}

	private List<TinyflowVectorMatch> toMatches(AiEmbeddingSearchResult result) {
		if (result == null || result.getMatches().isEmpty()) {
			return Collections.emptyList();
		}
		List<TinyflowVectorMatch> matches = new ArrayList<TinyflowVectorMatch>();
		for (AiEmbeddingMatch match : result.getMatches()) {
			AiEmbeddingDocument document = match.getDocument();
			matches.add(new TinyflowVectorMatch(match.getEmbeddingId(), document == null ? null : document.getText(),
					match.getScore(), document == null ? null : document.getMetadata()));
		}
		return matches;
	}

	private String resolveKey(String key) {
		if (key != null && key.trim().length() > 0) {
			return key.trim();
		}
		if (storeRegistry != null && storeRegistry.getDefaultKey() != null
				&& storeRegistry.getDefaultKey().trim().length() > 0) {
			return storeRegistry.getDefaultKey();
		}
		throw new TinyflowException("Tinyflow vector store key must not be blank");
	}
}
