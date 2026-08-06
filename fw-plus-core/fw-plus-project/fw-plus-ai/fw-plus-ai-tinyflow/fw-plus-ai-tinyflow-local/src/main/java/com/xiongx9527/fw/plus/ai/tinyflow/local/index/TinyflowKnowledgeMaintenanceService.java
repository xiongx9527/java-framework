package com.xiongx9527.fw.plus.ai.tinyflow.local.index;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Coordinates create, rebuild and source deletion while preserving an old active version on failure. */
public final class TinyflowKnowledgeMaintenanceService {

	public static final String VECTOR_STORE_KEY = "vectorStoreKey";

	private final TinyflowKnowledgeRepository repository;
	private final TinyflowLocalIndexer indexer;
	private final TinyflowVectorStoreProvider vectorStoreProvider;

	public TinyflowKnowledgeMaintenanceService(TinyflowKnowledgeRepository repository,
			TinyflowLocalIndexer indexer, TinyflowVectorStoreProvider vectorStoreProvider) {
		if (repository == null || indexer == null || vectorStoreProvider == null) {
			throw new IllegalArgumentException("Maintenance dependencies must not be null");
		}
		this.repository = repository;
		this.indexer = indexer;
		this.vectorStoreProvider = vectorStoreProvider;
	}

	public TinyflowKnowledgeDefinition create(TinyflowKnowledgeDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("definition must not be null");
		}
		if (repository.findKnowledge(definition.getTenantId(), definition.getKnowledgeId()) != null) {
			throw new TinyflowException("Knowledge already exists");
		}
		String storeKey = vectorStoreKey(definition);
		TinyflowVectorStoreDescriptor descriptor = descriptor(storeKey);
		try {
			repository.saveKnowledgeWithVectorStoreOwnership(
					definition, 0L, storeKey, descriptor.isMetadataFilterSupported());
		} catch (TinyflowKnowledgeRepository.VectorStoreOwnershipException exception) {
			throw new TinyflowException("Vector store does not support knowledge filtering: " + storeKey, exception);
		}
		return definition;
	}

	public void rebuild(String tenantId, String knowledgeId, String sourceId) {
		descriptor(vectorStoreKey(requireKnowledge(tenantId, knowledgeId)));
		TinyflowDocument active = repository.findActiveDocument(tenantId, knowledgeId, sourceId);
		if (active == null) {
			throw new TinyflowException("Active document was not found");
		}
		List<TinyflowChunk> sourceChunks = repository.findChunksByDocument(tenantId, knowledgeId, active.getDocumentId());
		if (sourceChunks.isEmpty()) {
			throw new TinyflowException("Active document has no chunks");
		}
		List<TinyflowDocument> oldVersions = repository.findDocumentVersions(tenantId, knowledgeId, sourceId);
		TinyflowDocument rebuilt = copyDocument(active, UUID.randomUUID().toString());
		List<TinyflowChunk> rebuiltChunks = copyChunks(sourceChunks, rebuilt.getDocumentId());
		List<TinyflowChunk> indexed = Collections.emptyList();
		try {
			indexed = indexer.indexForAtomicComplete(rebuilt, rebuiltChunks);
			repository.completeIngestion(rebuilt, indexed, task(rebuilt, "COMPLETED", null));
		} catch (RuntimeException exception) {
			if (!indexed.isEmpty()) {
				indexer.recordCompensation(rebuilt, vectorIds(indexed), TinyflowVectorCompensator.INDEX_ROLLBACK);
			}
			throw exception instanceof TinyflowException ? (TinyflowException) exception
					: new TinyflowException("Knowledge rebuild failed", exception);
		}
		TinyflowException cleanupFailure = null;
		for (TinyflowDocument oldVersion : oldVersions) {
			try {
				cleanupOldVersion(oldVersion);
			} catch (TinyflowException exception) {
				if (cleanupFailure == null) {
					cleanupFailure = exception;
				}
			}
		}
		if (cleanupFailure != null) {
			throw cleanupFailure;
		}
	}

	public void deleteSource(String tenantId, String knowledgeId, String sourceId) {
		TinyflowDocument active = repository.findActiveDocument(tenantId, knowledgeId, sourceId);
		if (active == null) {
			return;
		}
		descriptor(vectorStoreKey(requireKnowledge(tenantId, knowledgeId)));
		repository.saveTask(task(active, "DELETE_PENDING", "Deleting source vectors"));
		List<String> vectorIds = vectorIdsForSource(tenantId, knowledgeId, sourceId);
		TinyflowKnowledgeRepository.VectorCompensation compensation =
				indexer.recordCompensation(active, vectorIds, TinyflowVectorCompensator.DELETE_PENDING);
		try {
			new TinyflowVectorCompensator(repository, indexer).complete(compensation);
		} catch (Exception exception) {
			throw new TinyflowException("Knowledge source deletion is pending compensation", exception);
		}
	}

	private void cleanupOldVersion(TinyflowDocument document) {
		List<String> vectorIds = vectorIds(repository.findChunksByDocument(
				document.getTenantId(), document.getKnowledgeId(), document.getDocumentId()));
		TinyflowKnowledgeRepository.VectorCompensation compensation =
				indexer.recordCompensation(document, vectorIds, TinyflowVectorCompensator.DELETE_OLD_VERSION);
		try {
			new TinyflowVectorCompensator(repository, indexer).complete(compensation);
		} catch (Exception exception) {
			throw new TinyflowException("Old document version cleanup is pending compensation", exception);
		}
	}

	private TinyflowKnowledgeDefinition requireKnowledge(String tenantId, String knowledgeId) {
		TinyflowKnowledgeDefinition definition = repository.findKnowledge(tenantId, knowledgeId);
		if (definition == null) {
			throw new TinyflowException("Knowledge was not found");
		}
		return definition;
	}

	private TinyflowVectorStoreDescriptor descriptor(String storeKey) {
		for (TinyflowVectorStoreDescriptor descriptor : vectorStoreProvider.list()) {
			if (storeKey.equals(descriptor.getKey())) {
				if (vectorStoreProvider.get(storeKey) == null) {
					throw new TinyflowException("Vector store is unavailable: " + storeKey);
				}
				return descriptor;
			}
		}
		throw new TinyflowException("Vector store is not configured: " + storeKey);
	}

	private String vectorStoreKey(TinyflowKnowledgeDefinition definition) {
		Object value = definition.getMetadata().get(VECTOR_STORE_KEY);
		if (value == null || String.valueOf(value).trim().isEmpty()) {
			throw new TinyflowException("Knowledge vectorStoreKey is required");
		}
		return String.valueOf(value);
	}

	private TinyflowDocument copyDocument(TinyflowDocument source, String documentId) {
		return TinyflowDocument.builder().tenantId(source.getTenantId()).knowledgeId(source.getKnowledgeId())
				.sourceId(source.getSourceId()).sourceType(source.getSourceType()).documentId(documentId)
				.checksum(source.getChecksum()).name(source.getName()).content(source.getContent())
				.sourceUri(source.getSourceUri()).sourceVersion(source.getSourceVersion())
				.metadata(source.getMetadata()).build();
	}

	private List<TinyflowChunk> copyChunks(List<TinyflowChunk> source, String documentId) {
		List<TinyflowChunk> values = new ArrayList<TinyflowChunk>();
		for (TinyflowChunk chunk : source) {
			values.add(TinyflowChunk.builder().tenantId(chunk.getTenantId()).knowledgeId(chunk.getKnowledgeId())
					.sourceId(chunk.getSourceId()).sourceType(chunk.getSourceType()).documentId(documentId)
					.chunkId(UUID.randomUUID().toString()).content(chunk.getContent()).sourceUri(chunk.getSourceUri())
					.sourceVersion(chunk.getSourceVersion()).location(chunk.getLocation()).position(chunk.getPosition())
					.parentChunkId(chunk.getParentChunkId()).tokenCount(chunk.getTokenCount()).metadata(chunk.getMetadata())
					.enabled(chunk.isEnabled()).build());
		}
		return values;
	}

	private TinyflowIngestionTask task(TinyflowDocument document, String status, String message) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("sourceId", document.getSourceId());
		return TinyflowIngestionTask.builder().taskId(UUID.randomUUID().toString()).providerKey("fw-plus-local")
				.tenantId(document.getTenantId()).knowledgeId(document.getKnowledgeId()).documentId(document.getDocumentId())
				.status(status).message(message).metadata(metadata).build();
	}

	private List<String> vectorIds(List<TinyflowChunk> chunks) {
		List<String> values = new ArrayList<String>();
		for (TinyflowChunk chunk : chunks) {
			values.addAll(chunk.getVectorIds());
		}
		return values;
	}

	private List<String> vectorIdsForSource(String tenantId, String knowledgeId, String sourceId) {
		List<String> values = new ArrayList<String>();
		for (TinyflowDocument document : repository.findDocumentVersions(tenantId, knowledgeId, sourceId)) {
			values.addAll(vectorIds(repository.findChunksByDocument(tenantId, knowledgeId, document.getDocumentId())));
		}
		return values;
	}
}
