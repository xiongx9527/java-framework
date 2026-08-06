package com.xiongx9527.fw.plus.ai.tinyflow.local.index;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Retries durable vector cleanup and completes the resource transition recorded by each operation. */
public final class TinyflowVectorCompensator {
	static final String INDEX_ROLLBACK = "INDEX_ROLLBACK";
	static final String DELETE_OLD_VERSION = "DELETE_OLD_VERSION";
	static final String DELETE_PENDING = "DELETE_PENDING";

	private final TinyflowKnowledgeRepository repository;
	private final TinyflowLocalIndexer indexer;

	public TinyflowVectorCompensator(TinyflowKnowledgeRepository repository, TinyflowLocalIndexer indexer) {
		if (repository == null || indexer == null) {
			throw new IllegalArgumentException("repository and indexer must not be null");
		}
		this.repository = repository;
		this.indexer = indexer;
	}

	public void retry(String tenantId, String knowledgeId) {
		List<TinyflowKnowledgeRepository.VectorCompensation> compensations =
				repository.findVectorCompensations(tenantId, knowledgeId);
		for (TinyflowKnowledgeRepository.VectorCompensation compensation : compensations) {
			try {
				if (!isSupported(compensation.getOperation())) {
					continue;
				}
				indexer.delete(tenantId, knowledgeId, compensation.getVectorIds());
				if (DELETE_OLD_VERSION.equals(compensation.getOperation())) {
					repository.deleteDocumentVersion(tenantId, knowledgeId, compensation.getSourceId(),
							compensation.getDocumentId());
				} else if (DELETE_PENDING.equals(compensation.getOperation())) {
					repository.deleteDocumentsBySource(tenantId, knowledgeId, compensation.getSourceId());
					repository.saveTask(deletedTask(compensation));
				}
				repository.deleteVectorCompensation(tenantId, knowledgeId, compensation.getCompensationId());
			} catch (Exception ignored) {
				// The repository record remains the durable retry signal.
			}
		}
	}

	private boolean isSupported(String operation) {
		return INDEX_ROLLBACK.equals(operation) || DELETE_OLD_VERSION.equals(operation)
				|| DELETE_PENDING.equals(operation);
	}

	private TinyflowIngestionTask deletedTask(TinyflowKnowledgeRepository.VectorCompensation compensation) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("sourceId", compensation.getSourceId());
		return TinyflowIngestionTask.builder().taskId(UUID.randomUUID().toString()).providerKey("fw-plus-local")
				.tenantId(compensation.getTenantId()).knowledgeId(compensation.getKnowledgeId())
				.documentId(compensation.getDocumentId()).status("DELETED").metadata(metadata).build();
	}
}
