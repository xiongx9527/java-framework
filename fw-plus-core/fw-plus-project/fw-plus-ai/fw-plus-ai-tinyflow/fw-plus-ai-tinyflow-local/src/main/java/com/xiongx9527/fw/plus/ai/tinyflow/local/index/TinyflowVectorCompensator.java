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
				complete(compensation);
			} catch (Exception ignored) {
				// The repository record remains the durable retry signal.
			}
		}
	}

	void complete(TinyflowKnowledgeRepository.VectorCompensation compensation) throws Exception {
		if (compensation == null || !isSupported(compensation.getOperation())) {
			return;
		}
		String stage = compensation.getStage();
		if (TinyflowKnowledgeRepository.VectorCompensation.VECTOR_PENDING.equals(stage)) {
			indexer.delete(compensation.getTenantId(), compensation.getKnowledgeId(), compensation.getVectorIds());
			String nextStage = INDEX_ROLLBACK.equals(compensation.getOperation())
					? TinyflowKnowledgeRepository.VectorCompensation.COMPLETE
					: TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING;
			advance(compensation, stage, nextStage);
			stage = nextStage;
		}
		if (DELETE_OLD_VERSION.equals(compensation.getOperation())
				&& TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING.equals(stage)) {
			repository.deleteDocumentVersion(compensation.getTenantId(), compensation.getKnowledgeId(),
					compensation.getSourceId(), compensation.getDocumentId());
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
