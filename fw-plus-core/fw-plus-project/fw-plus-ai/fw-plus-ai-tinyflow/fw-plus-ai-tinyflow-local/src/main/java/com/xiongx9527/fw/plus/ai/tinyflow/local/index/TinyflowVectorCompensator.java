package com.xiongx9527.fw.plus.ai.tinyflow.local.index;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;

import java.util.List;

/** Retries durable orphan-vector cleanup without changing document visibility. */
public final class TinyflowVectorCompensator {

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
				indexer.delete(tenantId, knowledgeId, compensation.getVectorIds());
				repository.deleteVectorCompensation(tenantId, knowledgeId, compensation.getCompensationId());
			} catch (Exception ignored) {
				// The repository record remains the durable retry signal.
			}
		}
	}
}
