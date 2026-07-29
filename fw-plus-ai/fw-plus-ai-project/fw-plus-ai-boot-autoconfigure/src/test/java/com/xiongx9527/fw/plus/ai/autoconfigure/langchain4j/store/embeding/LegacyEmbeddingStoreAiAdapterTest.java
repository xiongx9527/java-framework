package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LegacyEmbeddingStoreAiAdapterTest {

	@Test
	public void shouldSupportOnlyLangChain4jDefinitions() {
		LegacyEmbeddingStoreAiAdapter adapter = new LegacyEmbeddingStoreAiAdapter(null,
				Collections.<EmbeddingStoreProviderAdapter>emptyList());
		AiEmbeddingStoreDefinition langChain4j = definition();
		AiEmbeddingStoreDefinition other = definition();
		other.setFramework("other");

		assertTrue(adapter.supports(langChain4j));
		assertFalse(adapter.supports(other));
	}

	@Test
	public void shouldRejectExplicitIdWhenContentOrMetadataCannotBePreserved() {
		EmbeddingStoreFactory legacyFactory = new EmbeddingStoreFactory() {
			@Override
			public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition,
					List<EmbeddingStoreProviderAdapter> adapters) {
				return new InMemoryEmbeddingStore<TextSegment>();
			}
		};
		LegacyEmbeddingStoreAiAdapter adapter = new LegacyEmbeddingStoreAiAdapter(legacyFactory,
				Collections.<EmbeddingStoreProviderAdapter>emptyList());
		AiEmbeddingStore store = adapter.create(definition());
		AiEmbeddingDocument document = new AiEmbeddingDocument("doc-1", "must-not-be-lost",
				Collections.<String, Object>singletonMap("tenant", "secret-tenant"),
				new AiEmbeddingVector(new float[] {1.0f, 2.0f}));

		AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
				() -> store.add(document));

		assertTrue(exception.getMessage().contains("explicit id"));
		assertFalse(exception.getMessage().contains("secret-tenant"));
	}

	private AiEmbeddingStoreDefinition definition() {
		AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
		definition.setKey("knowledge");
		definition.setFramework("langchain4j");
		definition.setProvider("in-memory");
		definition.setType(AiEmbeddingStoreType.VECTOR);
		definition.setEnabled(true);
		return definition;
	}
}
