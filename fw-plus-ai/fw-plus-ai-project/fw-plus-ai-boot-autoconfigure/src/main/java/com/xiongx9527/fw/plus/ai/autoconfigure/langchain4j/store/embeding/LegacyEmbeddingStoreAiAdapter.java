package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class LegacyEmbeddingStoreAiAdapter implements AiEmbeddingStoreAdapter {

	private final EmbeddingStoreFactory factory;
	private final List<EmbeddingStoreProviderAdapter> adapters;

	LegacyEmbeddingStoreAiAdapter(EmbeddingStoreFactory factory, List<EmbeddingStoreProviderAdapter> adapters) {
		this.factory = factory;
		this.adapters = adapters;
	}

	@Override
	public boolean supports(AiEmbeddingStoreDefinition definition) {
		return definition != null && "langchain4j".equalsIgnoreCase(definition.getFramework());
	}

	@Override
	public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition) {
		EmbeddingStoreDefinition legacy = new EmbeddingStoreDefinition();
		legacy.setKey(definition.getKey());
		legacy.setProvider(definition.getProvider());
		legacy.setEnabled(definition.isEnabled());
		legacy.setProperties(definition.getProperties());
		Object dimension = definition.getProperties().get("dimension");
		if (dimension != null) {
			legacy.setDimension(Integer.valueOf(String.valueOf(dimension)));
		}
		return new Facade(definition, factory.create(legacy, adapters));
	}

	private static final class Facade implements AiEmbeddingStore {

		private final AiEmbeddingStoreDefinition definition;
		private final EmbeddingStore<TextSegment> store;

		private Facade(AiEmbeddingStoreDefinition definition, EmbeddingStore<TextSegment> store) {
			this.definition = definition;
			this.store = store;
		}

		@Override public String getKey() { return definition.getKey(); }
		@Override public String getFramework() { return definition.getFramework(); }
		@Override public String getProvider() { return definition.getProvider(); }
		@Override public AiEmbeddingStoreType getType() { return definition.getType(); }

		@Override
		public String add(AiEmbeddingDocument document) {
			if (document == null || document.getVector() == null) {
				throw new AiEmbeddingStoreException("Embedding document and vector must not be null: key=" + getKey());
			}
			Embedding embedding = Embedding.from(document.getVector().toArray());
			if (document.getId() != null && !document.getId().trim().isEmpty()) {
				if (document.getText() != null
						|| (document.getMetadata() != null && !document.getMetadata().isEmpty())) {
					throw new AiEmbeddingStoreException("LangChain4j 0.35 cannot preserve content or metadata "
							+ "when adding an embedding with an explicit id: key=" + getKey());
				}
				store.add(document.getId(), embedding);
				return document.getId();
			}
			return store.add(embedding, TextSegment.from(document.getText() == null ? "" : document.getText(),
					new Metadata(document.getMetadata())));
		}

		@Override
		public List<String> addAll(List<AiEmbeddingDocument> documents) {
			if (documents == null) {
				return Collections.emptyList();
			}
			List<String> ids = new ArrayList<String>();
			for (AiEmbeddingDocument document : documents) {
				ids.add(add(document));
			}
			return ids;
		}

		@Override public void remove(String id) { store.remove(id); }
		@Override public void removeAll(List<String> ids) { store.removeAll(ids); }

		@Override
		public AiEmbeddingSearchResult search(AiEmbeddingQuery query) {
			if (query == null || query.getVector() == null) {
				throw new AiEmbeddingStoreException("Embedding query and vector must not be null: key=" + getKey());
			}
			dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> result = store.search(
					new EmbeddingSearchRequest(Embedding.from(query.getVector().toArray()), query.getMaxResults(),
							query.getMinScore(), null));
			List<AiEmbeddingMatch> matches = new ArrayList<AiEmbeddingMatch>();
			if (result != null && result.matches() != null) {
				for (dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match : result.matches()) {
					TextSegment segment = match.embedded();
					Map<String, Object> metadata = segment == null || segment.metadata() == null
							? Collections.<String, Object>emptyMap() : segment.metadata().toMap();
					AiEmbeddingVector vector = match.embedding() == null ? null
							: new AiEmbeddingVector(match.embedding().vector());
					AiEmbeddingDocument document = new AiEmbeddingDocument(match.embeddingId(),
							segment == null ? null : segment.text(), metadata, vector);
					matches.add(new AiEmbeddingMatch(match.score(), match.embeddingId(), document));
				}
			}
			return new AiEmbeddingSearchResult(matches);
		}
	}
}
