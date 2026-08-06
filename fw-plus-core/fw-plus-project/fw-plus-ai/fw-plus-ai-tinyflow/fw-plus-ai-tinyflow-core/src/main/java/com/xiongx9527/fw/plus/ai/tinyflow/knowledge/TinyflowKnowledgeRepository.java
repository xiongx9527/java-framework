package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface TinyflowKnowledgeRepository {

	TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId);

	long findKnowledgeVersion(String tenantId, String knowledgeId);

	void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion);

	void deleteKnowledge(String tenantId, String knowledgeId, long expectedVersion);

	TinyflowDocument findBySourceAndChecksum(String tenantId, String knowledgeId, String sourceId, String checksum);

	TinyflowDocument findActiveDocument(String tenantId, String knowledgeId, String sourceId);

	void saveDocument(TinyflowDocument document);

	List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId, String sourceId);

	/**
	 * Atomically switches the active document version. Implementations must validate the complete
	 * tenant/knowledge/source/document identity before changing state. A failed switch leaves the
	 * previous active version unchanged, and concurrent readers observe either the old or new version.
	 */
	void activateDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId);

	void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId);

	void saveChunks(List<TinyflowChunk> chunks);

	TinyflowChunk findChunk(String tenantId, String knowledgeId, String chunkId);

	List<TinyflowChunk> findChunksByDocument(String tenantId, String knowledgeId, String documentId);

	List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId, List<String> vectorIds);

	void updateChunk(String tenantId, String knowledgeId, String chunkId, boolean enabled,
			String content, Map<String, Object> metadata);

	void saveTask(TinyflowIngestionTask task);

	/**
	 * Atomically persists the indexed chunks, completed document, active document switch, and
	 * completed task. If any write fails, implementations must leave all four resources exactly as
	 * they were before this call so readers cannot observe a partially completed ingestion.
	 */
	void completeIngestion(TinyflowDocument completedDocument, List<TinyflowChunk> chunks,
			TinyflowIngestionTask completedTask);

	List<TinyflowIngestionTask> findTasks(String tenantId, String knowledgeId);

	void saveVectorCompensation(VectorCompensation compensation);

	List<VectorCompensation> findVectorCompensations(String tenantId, String knowledgeId);

	void deleteVectorCompensation(String tenantId, String knowledgeId, String compensationId);

	final class VectorCompensation {
		private final String compensationId;
		private final String tenantId;
		private final String knowledgeId;
		private final String sourceId;
		private final String documentId;
		private final List<String> vectorIds;
		private final String operation;

		public VectorCompensation(String compensationId, String tenantId, String knowledgeId,
				String sourceId, String documentId, List<String> vectorIds, String operation) {
			this.compensationId = TinyflowKnowledgeReference.requireText(compensationId, "compensationId");
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.knowledgeId = TinyflowKnowledgeReference.requireText(knowledgeId, "knowledgeId");
			this.sourceId = TinyflowKnowledgeReference.requireText(sourceId, "sourceId");
			this.documentId = TinyflowKnowledgeReference.requireText(documentId, "documentId");
			this.vectorIds = vectorIds == null ? Collections.<String>emptyList()
					: Collections.unmodifiableList(new ArrayList<String>(vectorIds));
			this.operation = TinyflowKnowledgeReference.requireText(operation, "operation");
		}

		public String getCompensationId() { return compensationId; }
		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getSourceId() { return sourceId; }
		public String getDocumentId() { return documentId; }
		public List<String> getVectorIds() { return vectorIds; }
		public String getOperation() { return operation; }
	}

	final class OptimisticLockException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final long expectedVersion;
		private final long actualVersion;

		public OptimisticLockException(long expectedVersion, long actualVersion) {
			super("Knowledge version conflict: expected " + expectedVersion + " but was " + actualVersion);
			this.expectedVersion = expectedVersion;
			this.actualVersion = actualVersion;
		}

		public long getExpectedVersion() { return expectedVersion; }
		public long getActualVersion() { return actualVersion; }
	}
}
