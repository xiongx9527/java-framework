package com.xiongx9527.fw.plus.ai.tinyflow.local.index;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.local.DefaultTinyflowIngestionService;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Retries durable vector cleanup and completes the resource transition recorded by each operation. */
public final class TinyflowVectorCompensator {
	static final String INDEX_ROLLBACK = "INDEX_ROLLBACK";
	public static final String DELETE_OLD_VERSION = "DELETE_OLD_VERSION";
	public static final String DELETE_FAILED_IMPORT = "DELETE_FAILED_IMPORT";
	public static final String DELETE_TEMPORARY_OBJECT = "DELETE_TEMPORARY_OBJECT";
	static final String DELETE_PENDING = "DELETE_PENDING";
	static final String DELETE_REPLACED_CHUNK_VECTORS = "DELETE_REPLACED_CHUNK_VECTORS";

	private final TinyflowKnowledgeRepository repository;
	private final DefaultTinyflowIngestionService.IngestionIndex indexer;
	private final TinyflowObjectStorage objectStorage;

	public TinyflowVectorCompensator(TinyflowKnowledgeRepository repository, TinyflowLocalIndexer indexer) {
		this(repository, (DefaultTinyflowIngestionService.IngestionIndex) indexer, null);
	}

	public TinyflowVectorCompensator(TinyflowKnowledgeRepository repository, TinyflowLocalIndexer indexer,
			TinyflowObjectStorage objectStorage) {
		this(repository, (DefaultTinyflowIngestionService.IngestionIndex) indexer, objectStorage);
	}

	public TinyflowVectorCompensator(TinyflowKnowledgeRepository repository,
			DefaultTinyflowIngestionService.IngestionIndex indexer) {
		this(repository, indexer, null);
	}

	public TinyflowVectorCompensator(TinyflowKnowledgeRepository repository,
			DefaultTinyflowIngestionService.IngestionIndex indexer, TinyflowObjectStorage objectStorage) {
		if (repository == null || indexer == null) {
			throw new IllegalArgumentException("repository and indexer must not be null");
		}
		this.repository = repository;
		this.indexer = indexer;
		this.objectStorage = objectStorage;
	}

	public void retry(String tenantId, String knowledgeId) {
		List<TinyflowKnowledgeRepository.VectorCompensation> compensations =
				repository.findVectorCompensations(tenantId, knowledgeId);
		for (TinyflowKnowledgeRepository.VectorCompensation compensation : compensations) {
			try {
				complete(compensation);
			} catch (Exception ignored) {
				// The repository record remains the durable retry signal.
			}
		}
	}

	public void complete(TinyflowKnowledgeRepository.VectorCompensation compensation) throws Exception {
		if (compensation == null || !isSupported(compensation.getOperation())) {
			return;
		}
		String stage = compensation.getStage();
		if (TinyflowKnowledgeRepository.VectorCompensation.VECTOR_PENDING.equals(stage)) {
			indexer.delete(compensation.getTenantId(), compensation.getKnowledgeId(), compensation.getVectorIds());
			String nextStage = INDEX_ROLLBACK.equals(compensation.getOperation())
					|| DELETE_REPLACED_CHUNK_VECTORS.equals(compensation.getOperation())
					? TinyflowKnowledgeRepository.VectorCompensation.COMPLETE
					: TinyflowKnowledgeRepository.VectorCompensation.OBJECT_PENDING;
			advance(compensation, stage, nextStage);
			stage = nextStage;
		}
		if ((DELETE_OLD_VERSION.equals(compensation.getOperation())
				|| DELETE_FAILED_IMPORT.equals(compensation.getOperation()))
				&& TinyflowKnowledgeRepository.VectorCompensation.OBJECT_PENDING.equals(stage)) {
			deleteStoredObject(document(compensation));
			advance(compensation, stage, TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING);
			stage = TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING;
		}
		if (DELETE_TEMPORARY_OBJECT.equals(compensation.getOperation())
				&& TinyflowKnowledgeRepository.VectorCompensation.OBJECT_PENDING.equals(stage)) {
			deleteStoredObject(compensation.getTenantId(), compensation.getObjectKey());
			advance(compensation, stage, TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING);
			stage = TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING;
		}
		if (DELETE_PENDING.equals(compensation.getOperation())
				&& TinyflowKnowledgeRepository.VectorCompensation.OBJECT_PENDING.equals(stage)) {
			deleteStoredObjects(repository.findDocumentVersions(
					compensation.getTenantId(), compensation.getKnowledgeId(), compensation.getSourceId()));
			advance(compensation, stage, TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING);
			stage = TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING;
		}
		if ((DELETE_OLD_VERSION.equals(compensation.getOperation())
				|| DELETE_FAILED_IMPORT.equals(compensation.getOperation()))
				&& TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING.equals(stage)) {
			repository.deleteDocumentVersion(compensation.getTenantId(), compensation.getKnowledgeId(),
					compensation.getSourceId(), compensation.getDocumentId());
			advance(compensation, stage, TinyflowKnowledgeRepository.VectorCompensation.COMPLETE);
			stage = TinyflowKnowledgeRepository.VectorCompensation.COMPLETE;
		}
		if (DELETE_TEMPORARY_OBJECT.equals(compensation.getOperation())
				&& TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING.equals(stage)) {
			advance(compensation, stage, TinyflowKnowledgeRepository.VectorCompensation.COMPLETE);
			stage = TinyflowKnowledgeRepository.VectorCompensation.COMPLETE;
		}
		if (DELETE_PENDING.equals(compensation.getOperation())
				&& TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING.equals(stage)) {
			repository.deleteDocumentsBySource(
					compensation.getTenantId(), compensation.getKnowledgeId(), compensation.getSourceId());
			advance(compensation, stage, TinyflowKnowledgeRepository.VectorCompensation.TASK_PENDING);
			stage = TinyflowKnowledgeRepository.VectorCompensation.TASK_PENDING;
		}
		if (DELETE_PENDING.equals(compensation.getOperation())
				&& TinyflowKnowledgeRepository.VectorCompensation.TASK_PENDING.equals(stage)) {
			repository.saveTask(deletedTask(compensation));
			advance(compensation, stage, TinyflowKnowledgeRepository.VectorCompensation.COMPLETE);
			stage = TinyflowKnowledgeRepository.VectorCompensation.COMPLETE;
		}
		if (TinyflowKnowledgeRepository.VectorCompensation.COMPLETE.equals(stage)) {
			repository.deleteVectorCompensation(compensation.getTenantId(), compensation.getKnowledgeId(),
					compensation.getCompensationId());
		}
	}

	private void advance(TinyflowKnowledgeRepository.VectorCompensation compensation,
			String expectedStage, String nextStage) {
		repository.advanceVectorCompensationStage(compensation.getTenantId(), compensation.getKnowledgeId(),
				compensation.getCompensationId(), expectedStage, nextStage);
	}

	private boolean isSupported(String operation) {
		return INDEX_ROLLBACK.equals(operation) || DELETE_OLD_VERSION.equals(operation)
				|| DELETE_FAILED_IMPORT.equals(operation) || DELETE_PENDING.equals(operation)
				|| DELETE_TEMPORARY_OBJECT.equals(operation)
				|| DELETE_REPLACED_CHUNK_VECTORS.equals(operation);
	}

	private TinyflowDocument document(TinyflowKnowledgeRepository.VectorCompensation compensation) {
		for (TinyflowDocument document : repository.findDocumentVersions(compensation.getTenantId(),
				compensation.getKnowledgeId(), compensation.getSourceId())) {
			if (compensation.getDocumentId().equals(document.getDocumentId())) {
				return document;
			}
		}
		return null;
	}

	private void deleteStoredObjects(List<TinyflowDocument> documents) {
		Set<String> deleted = new LinkedHashSet<String>();
		for (TinyflowDocument document : documents) {
			if (document != null && document.getSourceUri() != null && deleted.add(document.getSourceUri())) {
				deleteStoredObject(document);
			}
		}
	}

	private void deleteStoredObject(TinyflowDocument document) {
		if (objectStorage != null && document != null && document.getSourceUri() != null
				&& document.getMetadata().containsKey("s3Key")) {
			objectStorage.delete(document.getTenantId(), document.getSourceUri());
		}
	}

	private void deleteStoredObject(String tenantId, String objectKey) {
		if (objectStorage == null) throw new IllegalStateException("Object storage is unavailable");
		if (objectKey == null || objectKey.trim().length() == 0) {
			throw new IllegalArgumentException("Object key must not be blank");
		}
		objectStorage.delete(tenantId, objectKey);
	}

	private TinyflowIngestionTask deletedTask(TinyflowKnowledgeRepository.VectorCompensation compensation) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("sourceId", compensation.getSourceId());
		return TinyflowIngestionTask.builder().taskId(UUID.randomUUID().toString()).providerKey("fw-plus-local")
				.tenantId(compensation.getTenantId()).knowledgeId(compensation.getKnowledgeId())
				.documentId(compensation.getDocumentId()).status("DELETED").metadata(metadata).build();
	}
}
