package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TinyEngineKnowledgeBaseServiceTest {

	@Test
	public void shouldSkipUnsupportedFormatAndReportMissingPath() {
		RecordingStore store = new RecordingStore();
		DefaultTinyEngineKnowledgeBaseService service = new DefaultTinyEngineKnowledgeBaseService(new Registry(store), "knowledge", 4, 1);
		TinyEngineKnowledgeBaseDocument unsupported = document("memory.bin", "data", "bin", "tiny");
		TinyEngineKnowledgeBaseDocument missing = document("/missing/tiny.txt", null, "txt", "tiny");

		assertEquals(1, service.index(unsupported).getSkipped());
		TinyEngineKnowledgeBaseIndexResult missingResult = service.index(missing);
		assertEquals(1, missingResult.getFailed());
		assertEquals("/missing/tiny.txt", missingResult.getFailures().get(0));
	}

	@Test
	public void shouldChunkTextAndIncludeRequiredMetadata() {
		RecordingStore store = new RecordingStore();
		DefaultTinyEngineKnowledgeBaseService service = new DefaultTinyEngineKnowledgeBaseService(new Registry(store), "knowledge", 4, 1);

		TinyEngineKnowledgeBaseIndexResult result = service.index(document("classpath:guide.txt", "abcdefgh", "text", "tiny"));

		assertEquals(1, result.getLoaded());
		assertEquals(3, result.getChunks());
		assertEquals(Arrays.asList("abcd", "defg", "gh"), texts(store.documents));
		assertEquals("classpath:guide.txt", store.documents.get(0).getMetadata().get("source"));
		assertEquals("text", store.documents.get(0).getMetadata().get("format"));
		assertEquals("tiny", store.documents.get(0).getMetadata().get("collection"));
		assertEquals(true, store.documents.get(0).getMetadata().containsKey("timestamp"));
	}

	@Test
	public void shouldReadTextResourceAndChunkItWithConfiguredOverlap() throws Exception {
		RecordingStore store = new RecordingStore();
		DefaultTinyEngineKnowledgeBaseService service = new DefaultTinyEngineKnowledgeBaseService(new Registry(store), "knowledge", 4, 1);
		File resource = File.createTempFile("tiny-engine-rag", ".txt");
		try {
			Files.write(resource.toPath(), "abcdefgh".getBytes(StandardCharsets.UTF_8));
			TinyEngineKnowledgeBaseIndexResult result = service.index(document(resource.getAbsolutePath(), null, "txt", "tiny"));
			assertEquals(1, result.getLoaded());
			assertEquals(Arrays.asList("abcd", "defg", "gh"), texts(store.documents));
		} finally {
			resource.delete();
		}
	}

	private TinyEngineKnowledgeBaseDocument document(String source, String content, String format, String collection) {
		TinyEngineKnowledgeBaseDocument document = new TinyEngineKnowledgeBaseDocument();
		document.setSource(source);
		document.setContent(content);
		document.setFormat(format);
		document.setCollection(collection);
		return document;
	}

	private List<String> texts(List<AiEmbeddingDocument> documents) {
		List<String> texts = new ArrayList<String>();
		for (AiEmbeddingDocument document : documents) { texts.add(document.getText()); }
		return texts;
	}

	private static final class Registry implements AiEmbeddingStoreRegistry {
		private final AiEmbeddingStore store;
		private Registry(AiEmbeddingStore store) { this.store = store; }
		@Override public AiEmbeddingStore get(String key) { return store; }
		@Override public AiEmbeddingStore getDefault() { return store; }
		@Override public String getDefaultKey() { return "knowledge"; }
		@Override public boolean contains(String key) { return true; }
		@Override public Set<String> keys() { return Collections.singleton("knowledge"); }
	}

	private static final class RecordingStore implements AiEmbeddingStore {
		private final List<AiEmbeddingDocument> documents = new ArrayList<AiEmbeddingDocument>();
		@Override public String getKey() { return "knowledge"; }
		@Override public String getFramework() { return "test"; }
		@Override public String getProvider() { return "test"; }
		@Override public AiEmbeddingStoreType getType() { return AiEmbeddingStoreType.VECTOR; }
		@Override public String add(AiEmbeddingDocument document) { documents.add(document); return document.getId(); }
		@Override public List<String> addAll(List<AiEmbeddingDocument> documents) { this.documents.addAll(documents); return Collections.emptyList(); }
		@Override public void remove(String id) { }
		@Override public void removeAll(List<String> ids) { }
		@Override public AiEmbeddingSearchResult search(AiEmbeddingQuery query) { return new AiEmbeddingSearchResult(Collections.emptyList()); }
	}
}
