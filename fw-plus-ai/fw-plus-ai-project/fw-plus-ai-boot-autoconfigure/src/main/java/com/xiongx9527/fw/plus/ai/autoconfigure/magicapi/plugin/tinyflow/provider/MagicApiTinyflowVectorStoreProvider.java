package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
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

public final class MagicApiTinyflowVectorStoreProvider implements TinyflowVectorStoreProvider {

	private final MagicDynamicVectorDataSource dataSource;

	public MagicApiTinyflowVectorStoreProvider(MagicDynamicVectorDataSource dataSource) {
		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource must not be null");
		}
		this.dataSource = dataSource;
	}

	@Override
	public List<TinyflowVectorStoreDescriptor> list() {
		List<TinyflowVectorStoreDescriptor> result = new ArrayList<TinyflowVectorStoreDescriptor>();
		for (String key : dataSource.keys()) {
			AiEmbeddingStore store = dataSource.get(key);
			if (store != null) {
				result.add(new TinyflowVectorStoreDescriptor(key, key, store.getType().name(), store.getProvider(), false));
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public TinyflowVectorStore get(String key) {
		final AiEmbeddingStore store = store(key);
		return new TinyflowVectorStore() {
			@Override
			public List<String> addAll(List<TinyflowVectorDocument> documents) {
				return store.addAll(documents(documents));
			}

			@Override
			public void removeAll(List<String> ids) {
				store.removeAll(ids);
			}

			@Override
			public List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore) {
				return matches(store.search(new AiEmbeddingQuery(new AiEmbeddingVector(vector), maxResults, minScore)));
			}
		};
	}

	private AiEmbeddingStore store(String key) {
		AiEmbeddingStore store = hasText(key) ? dataSource.get(key.trim()) : dataSource.getDefault();
		if (store == null) {
			throw new TinyflowException("Tinyflow vector store not found: " + key);
		}
		return store;
	}

	private List<AiEmbeddingDocument> documents(List<TinyflowVectorDocument> documents) {
		if (documents == null || documents.isEmpty()) {
			return Collections.emptyList();
		}
		List<AiEmbeddingDocument> result = new ArrayList<AiEmbeddingDocument>();
		for (TinyflowVectorDocument document : documents) {
			result.add(new AiEmbeddingDocument(document.getId(), document.getContent(), document.getMetadata(),
					new AiEmbeddingVector(document.getVector())));
		}
		return result;
	}

	private List<TinyflowVectorMatch> matches(AiEmbeddingSearchResult result) {
		if (result == null || result.getMatches().isEmpty()) {
			return Collections.emptyList();
		}
		List<TinyflowVectorMatch> converted = new ArrayList<TinyflowVectorMatch>();
		for (AiEmbeddingMatch match : result.getMatches()) {
			AiEmbeddingDocument document = match.getDocument();
			converted.add(new TinyflowVectorMatch(match.getEmbeddingId(), document == null ? null : document.getText(),
					match.getScore(), document == null ? null : document.getMetadata()));
		}
		return converted;
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}
}
