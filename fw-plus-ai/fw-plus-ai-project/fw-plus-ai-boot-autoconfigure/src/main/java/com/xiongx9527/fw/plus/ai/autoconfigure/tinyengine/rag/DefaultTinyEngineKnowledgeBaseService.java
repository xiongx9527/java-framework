package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineKnowledgeBaseService;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultTinyEngineKnowledgeBaseService implements TinyEngineKnowledgeBaseService {

	private final AiEmbeddingStoreRegistry storeRegistry;
	private final String storeKey;
	private final int chunkSize;
	private final int chunkOverlap;

	public DefaultTinyEngineKnowledgeBaseService(AiEmbeddingStoreRegistry storeRegistry, String storeKey, int chunkSize,
			int chunkOverlap) {
		this.storeRegistry = storeRegistry;
		this.storeKey = storeKey;
		this.chunkSize = chunkSize;
		this.chunkOverlap = chunkOverlap;
	}

	@Override
	public TinyEngineKnowledgeBaseIndexResult index(TinyEngineKnowledgeBaseDocument document) {
		if (document == null || !isSupportedFormat(document.getFormat(), document.getSource())) {
			return new TinyEngineKnowledgeBaseIndexResult(0, 1, 0, 0, Collections.<String>emptyList());
		}
		String content;
		try {
			content = isBlank(document.getContent()) ? readContent(document.getSource()) : document.getContent();
		} catch (IOException ex) {
			return failed(document.getSource());
		}
		if (content == null) {
			return failed(document.getSource());
		}
		List<AiEmbeddingDocument> chunks = chunks(document, content);
		if (!chunks.isEmpty()) {
			resolveStore().addAll(chunks);
		}
		return new TinyEngineKnowledgeBaseIndexResult(1, 0, 0, chunks.size(), Collections.<String>emptyList());
	}

	private TinyEngineKnowledgeBaseIndexResult failed(String source) {
		return new TinyEngineKnowledgeBaseIndexResult(0, 0, 1, 0, Collections.singletonList(source));
	}

	private List<AiEmbeddingDocument> chunks(TinyEngineKnowledgeBaseDocument document, String content) {
		int size = chunkSize > 0 ? chunkSize : 500;
		int overlap = chunkOverlap >= 0 && chunkOverlap < size ? chunkOverlap : 0;
		int step = size - overlap;
		List<AiEmbeddingDocument> documents = new ArrayList<AiEmbeddingDocument>();
		for (int start = 0, part = 0; start < content.length(); start += step, part++) {
			int end = Math.min(content.length(), start + size);
			Map<String, Object> metadata = new LinkedHashMap<String, Object>();
			metadata.put("source", document.getSource());
			metadata.put("format", normalizedFormat(document.getFormat(), document.getSource()));
			metadata.put("timestamp", Long.valueOf(System.currentTimeMillis()));
			metadata.put("collection", document.getCollection());
			documents.add(new AiEmbeddingDocument(document.getSource() + "#" + part, content.substring(start, end), metadata, null));
		}
		return documents;
	}

	private AiEmbeddingStore resolveStore() {
		if (storeRegistry == null) {
			throw new IllegalStateException("TinyEngine knowledge base embedding store registry is unavailable");
		}
		AiEmbeddingStore store = isBlank(storeKey) ? storeRegistry.getDefault() : storeRegistry.get(storeKey);
		if (store == null) {
			throw new IllegalStateException("TinyEngine knowledge base embedding store is unavailable");
		}
		return store;
	}

	private String readContent(String source) throws IOException {
		if (isBlank(source)) {
			throw new IOException("source is missing");
		}
		InputStream inputStream;
		if (source.startsWith("classpath:")) {
			ClassPathResource resource = new ClassPathResource(source.substring("classpath:".length()));
			if (!resource.exists()) { throw new IOException("resource does not exist"); }
			inputStream = resource.getInputStream();
		} else {
			File file = new File(source);
			if (!file.isFile()) { throw new IOException("file does not exist"); }
			inputStream = new FileInputStream(file);
		}
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int length;
			while ((length = inputStream.read(buffer)) != -1) { output.write(buffer, 0, length); }
			return new String(output.toByteArray(), "UTF-8");
		} finally {
			inputStream.close();
		}
	}

	private boolean isSupportedFormat(String format, String source) {
		String normalized = normalizedFormat(format, source);
		return "txt".equals(normalized) || "text".equals(normalized) || "md".equals(normalized)
				|| "markdown".equals(normalized) || "csv".equals(normalized) || "json".equals(normalized)
				|| "xml".equals(normalized);
	}

	private String normalizedFormat(String format, String source) {
		if (!isBlank(format)) { return format.trim().toLowerCase(); }
		if (source != null) {
			int dot = source.lastIndexOf('.');
			if (dot >= 0 && dot < source.length() - 1) { return source.substring(dot + 1).toLowerCase(); }
		}
		return "";
	}

	private boolean isBlank(String value) { return value == null || value.trim().length() == 0; }
}
