package com.xiongx9527.fw.plus.ai.tinyflow.local.index;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.local.DefaultTinyflowIngestionService;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local provider implementation of the ingestion index boundary. It keeps embeddings only until
 * their matching index batch is written, while the durable vector id mapping remains in the knowledge repository.
 */
public final class TinyflowLocalIndexer implements DefaultTinyflowIngestionService.IngestionIndex {

	private final TinyflowKnowledgeRepository repository;
	private final TinyflowModelProvider modelProvider;
	private final TinyflowVectorStoreProvider vectorStoreProvider;
	private final String embeddingModelKey;
	private final String vectorStoreKey;
	private final int batchSize;
	private final Map<String, Map<String, float[]>> embeddings = new ConcurrentHashMap<String, Map<String, float[]>>();

	public TinyflowLocalIndexer(TinyflowKnowledgeRepository repository, TinyflowModelProvider modelProvider,
			TinyflowVectorStoreProvider vectorStoreProvider, String embeddingModelKey, String vectorStoreKey,
			int batchSize) {
		if (repository == null || modelProvider == null || vectorStoreProvider == null
				|| isBlank(embeddingModelKey) || isBlank(vectorStoreKey) || batchSize <= 0) {
			throw new IllegalArgumentException("Indexer dependencies must not be null or blank");
		}
		this.repository = repository;
		this.modelProvider = modelProvider;
		this.vectorStoreProvider = vectorStoreProvider;
		this.embeddingModelKey = embeddingModelKey;
		this.vectorStoreKey = vectorStoreKey;
		this.batchSize = batchSize;
	}

	public String getVectorStoreKey() {
		return vectorStoreKey;
	}

	/**
	 * Indexes a standalone document and durably records its chunk/vector mapping. The ingestion
	 * service uses the lower-level methods below so its completeIngestion transaction remains the activation point.
	 */
	public List<TinyflowChunk> index(TinyflowDocument document, List<TinyflowChunk> chunks) {
		if (document == null) {
			throw new IllegalArgumentException("document must not be null");
		}
		try {
			embed(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId(), chunks);
			Map<String, List<String>> indexed = index(document.getTenantId(), document.getKnowledgeId(),
					document.getDocumentId(), chunks);
			List<TinyflowChunk> mapped = withVectorIds(chunks, indexed);
			try {
				repository.saveChunks(mapped);
			} catch (RuntimeException exception) {
				recordCompensation(document, collectVectorIds(indexed), TinyflowVectorCompensator.INDEX_ROLLBACK);
				throw new TinyflowException("Failed to persist indexed chunks", exception);
			}
			return mapped;
		} catch (DefaultTinyflowIngestionService.IngestionIndex.PartialIndexException exception) {
			recordCompensation(document, collectVectorIds(exception.getPartialResult()),
					TinyflowVectorCompensator.INDEX_ROLLBACK);
			throw new TinyflowException("Failed to write vector index", exception);
		} catch (Exception exception) {
			throw exception instanceof TinyflowException ? (TinyflowException) exception
					: new TinyflowException("Failed to prepare vector index", exception);
		} finally {
			embeddings.remove(documentKey(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId()));
		}
	}

	List<TinyflowChunk> indexForAtomicComplete(TinyflowDocument document, List<TinyflowChunk> chunks) {
		try {
			embed(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId(), chunks);
			return withVectorIds(chunks, index(document.getTenantId(), document.getKnowledgeId(),
					document.getDocumentId(), chunks));
		} catch (DefaultTinyflowIngestionService.IngestionIndex.PartialIndexException exception) {
			recordCompensation(document, collectVectorIds(exception.getPartialResult()),
					TinyflowVectorCompensator.INDEX_ROLLBACK);
			throw new TinyflowException("Failed to write vector index", exception);
		} catch (Exception exception) {
			throw exception instanceof TinyflowException ? (TinyflowException) exception
					: new TinyflowException("Failed to prepare vector index", exception);
		}
	}

	@Override
	public void embed(String tenantId, String knowledgeId, String documentId, List<TinyflowChunk> chunks) throws Exception {
		TinyflowEmbeddingModel model = modelProvider.getEmbeddingModel(embeddingModelKey);
		if (model == null) {
			throw new TinyflowException("Embedding model is unavailable: " + embeddingModelKey);
		}
		Map<String, float[]> values = new LinkedHashMap<String, float[]>();
		for (TinyflowChunk chunk : safeChunks(chunks)) {
			verifyChunkIdentity(tenantId, knowledgeId, documentId, chunk);
			float[] vector = model.embed(chunk.getContent());
			if (vector == null || vector.length == 0) {
				throw new TinyflowException("Embedding model returned an empty vector for chunk " + chunk.getChunkId());
			}
			values.put(chunk.getChunkId(), vector.clone());
		}
		embeddings.put(documentKey(tenantId, knowledgeId, documentId), values);
	}

	@Override
	public Map<String, List<String>> index(String tenantId, String knowledgeId, String documentId,
			List<TinyflowChunk> chunks) throws Exception {
		String resolvedStoreKey = resolveVectorStoreKey(tenantId, knowledgeId);
		TinyflowVectorStore store = vectorStoreProvider.get(resolvedStoreKey);
		if (store == null) {
			throw new TinyflowException("Vector store is unavailable: " + resolvedStoreKey);
		}
		Map<String, float[]> values = embeddings.get(documentKey(tenantId, knowledgeId, documentId));
		if (values == null) {
			throw new TinyflowException("Embeddings are unavailable for document " + documentId);
		}
		Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
		List<TinyflowChunk> source = safeChunks(chunks);
		try {
			for (int from = 0; from < source.size(); from += batchSize) {
				int to = Math.min(source.size(), from + batchSize);
				List<TinyflowChunk> batch = source.subList(from, to);
				for (TinyflowChunk chunk : batch) {
					verifyChunkIdentity(tenantId, knowledgeId, documentId, chunk);
					float[] vector = values.get(chunk.getChunkId());
					if (vector == null) {
						throw new TinyflowException("Embedding is unavailable for chunk " + chunk.getChunkId());
					}
					String vectorId = store.add(new TinyflowVectorDocument(
							chunk.getChunkId(), chunk.getContent(), vector, metadata(chunk)));
					if (isBlank(vectorId)) {
						throw new TinyflowException("Vector store returned an invalid vector id mapping");
					}
					result.put(chunk.getChunkId(), Collections.singletonList(vectorId));
				}
			}
			return result;
		} catch (Exception exception) {
			throw new DefaultTinyflowIngestionService.IngestionIndex.PartialIndexException(
					"Vector index write failed", result);
		} finally {
			embeddings.remove(documentKey(tenantId, knowledgeId, documentId));
		}
	}

	@Override
	public void delete(String tenantId, String knowledgeId, List<String> vectorIds) throws Exception {
		if (vectorIds == null || vectorIds.isEmpty()) {
			return;
		}
		String resolvedStoreKey = resolveVectorStoreKey(tenantId, knowledgeId);
		TinyflowVectorStore store = vectorStoreProvider.get(resolvedStoreKey);
		if (store == null) {
			throw new TinyflowException("Vector store is unavailable: " + resolvedStoreKey);
		}
		store.removeAll(new ArrayList<String>(new LinkedHashSet<String>(vectorIds)));
	}

	void recordCompensation(TinyflowDocument document, List<String> vectorIds, String operation) {
		if ((vectorIds == null || vectorIds.isEmpty())
				&& TinyflowVectorCompensator.INDEX_ROLLBACK.equals(operation)) {
			return;
		}
		repository.saveVectorCompensation(new TinyflowKnowledgeRepository.VectorCompensation(
				UUID.randomUUID().toString(), document.getTenantId(), document.getKnowledgeId(),
				document.getSourceId(), document.getDocumentId(),
				vectorIds == null ? Collections.<String>emptyList() : vectorIds, operation));
	}

	private List<TinyflowChunk> withVectorIds(List<TinyflowChunk> chunks, Map<String, List<String>> indexed) {
		List<TinyflowChunk> mapped = new ArrayList<TinyflowChunk>();
		for (TinyflowChunk chunk : safeChunks(chunks)) {
			List<String> vectorIds = indexed.get(chunk.getChunkId());
			if (vectorIds == null || vectorIds.isEmpty()) {
				throw new TinyflowException("Vector id mapping is incomplete");
			}
			mapped.add(TinyflowChunk.builder().tenantId(chunk.getTenantId()).knowledgeId(chunk.getKnowledgeId())
					.sourceId(chunk.getSourceId()).sourceType(chunk.getSourceType()).documentId(chunk.getDocumentId())
					.chunkId(chunk.getChunkId()).content(chunk.getContent()).sourceUri(chunk.getSourceUri())
					.sourceVersion(chunk.getSourceVersion()).location(chunk.getLocation()).position(chunk.getPosition())
					.parentChunkId(chunk.getParentChunkId()).tokenCount(chunk.getTokenCount()).metadata(chunk.getMetadata())
					.enabled(chunk.isEnabled()).vectorIds(vectorIds).build());
		}
		return mapped;
	}

	private Map<String, Object> metadata(TinyflowChunk chunk) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>(chunk.getMetadata());
		metadata.put("knowledge_id", chunk.getKnowledgeId());
		metadata.put("source_id", chunk.getSourceId());
		metadata.put("source_type", chunk.getSourceType());
		metadata.put("document_id", chunk.getDocumentId());
		metadata.put("chunk_id", chunk.getChunkId());
		return metadata;
	}

	private List<String> collectVectorIds(Map<String, List<String>> indexed) {
		Set<String> values = new LinkedHashSet<String>();
		for (List<String> ids : indexed.values()) {
			if (ids != null) {
				values.addAll(ids);
			}
		}
		return new ArrayList<String>(values);
	}

	private String resolveVectorStoreKey(String tenantId, String knowledgeId) {
		TinyflowKnowledgeDefinition definition = repository.findKnowledge(tenantId, knowledgeId);
		if (definition == null) {
			return vectorStoreKey;
		}
		Object configured = definition.getMetadata().get(TinyflowKnowledgeMaintenanceService.VECTOR_STORE_KEY);
		if (configured == null || isBlank(String.valueOf(configured))) {
			throw new TinyflowException("Knowledge vectorStoreKey is required");
		}
		return String.valueOf(configured);
	}

	private List<TinyflowChunk> safeChunks(List<TinyflowChunk> chunks) {
		return chunks == null ? Collections.<TinyflowChunk>emptyList() : chunks;
	}

	private void verifyChunkIdentity(String tenantId, String knowledgeId, String documentId, TinyflowChunk chunk) {
		if (!tenantId.equals(chunk.getTenantId()) || !knowledgeId.equals(chunk.getKnowledgeId())
				|| !documentId.equals(chunk.getDocumentId())) {
			throw new TinyflowException("Chunk identity does not match index request");
		}
	}

	private static String documentKey(String tenantId, String knowledgeId, String documentId) {
		return tenantId + "|" + knowledgeId + "|" + documentId;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
