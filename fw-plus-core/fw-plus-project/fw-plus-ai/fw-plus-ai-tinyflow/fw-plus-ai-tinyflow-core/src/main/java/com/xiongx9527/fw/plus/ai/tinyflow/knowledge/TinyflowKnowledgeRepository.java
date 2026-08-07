package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import com.xiongx9527.fw.plus.common.exception.BizException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface TinyflowKnowledgeRepository {

	TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId);

	long findKnowledgeVersion(String tenantId, String knowledgeId);

	void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion);

	/**
	 * Atomically validates global vector-store ownership and saves the knowledge definition. A store
	 * may have multiple owners only when metadata filtering is supported. Implementations must perform
	 * the ownership check and the versioned save in one transaction or critical section.
	 */
	void saveKnowledgeWithVectorStoreOwnership(TinyflowKnowledgeDefinition definition, long expectedVersion,
			String vectorStoreKey, boolean metadataFilterSupported);

	void deleteKnowledge(String tenantId, String knowledgeId, long expectedVersion);

	TinyflowDocument findBySourceAndChecksum(String tenantId, String knowledgeId, String sourceId, String checksum);

	TinyflowDocument findActiveDocument(String tenantId, String knowledgeId, String sourceId);

	void saveDocument(TinyflowDocument document);

	List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId, String sourceId);

	/** Returns every document version owned by one knowledge definition. */
	default List<TinyflowDocument> findDocumentsByKnowledge(String tenantId, String knowledgeId) {
		throw new UnsupportedOperationException("Knowledge document enumeration is not supported");
	}

	/**
	 * Atomically switches the active document version. Implementations must validate the complete
	 * tenant/knowledge/source/document identity before changing state. A failed switch leaves the
	 * previous active version unchanged, and concurrent readers observe either the old or new version.
	 */
	void activateDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId);

	void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId);

	/** Deletes one inactive document version and all of its chunks. Repeating a completed deletion is a no-op. */
	void deleteDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId);

	void saveChunks(List<TinyflowChunk> chunks);

	TinyflowChunk findChunk(String tenantId, String knowledgeId, String chunkId);

	List<TinyflowChunk> findChunksByDocument(String tenantId, String knowledgeId, String documentId);

	List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId, List<String> vectorIds);

	/**
	 * Searches keyword candidates within one tenant and knowledge boundary. Implementations return at
	 * most {@code limit} matches, ordered by descending relevance, with finite scores between 0 and 1.
	 */
	List<TinyflowKnowledgeMatch> searchChunksByKeyword(
			String tenantId, String knowledgeId, String query, int limit);

	void updateChunk(String tenantId, String knowledgeId, String chunkId, boolean enabled,
			String content, Map<String, Object> metadata);

	/** Atomically replaces one chunk and persists the compensation for its superseded vectors. */
	default void replaceChunkWithVectorCompensation(TinyflowChunk chunk, VectorCompensation compensation) {
		throw new UnsupportedOperationException("Atomic chunk replacement is not supported");
	}

	void saveTask(TinyflowIngestionTask task);

	/**
	 * Atomically persists a pending task and its durable vector compensation. If either write fails,
	 * implementations must leave both collections unchanged.
	 */
	void saveTaskWithVectorCompensation(TinyflowIngestionTask task, VectorCompensation compensation);

	/**
	 * Atomically persists the indexed chunks, completed document, active document switch, and
	 * completed task. If any write fails, implementations must leave all four resources exactly as
	 * they were before this call so readers cannot observe a partially completed ingestion.
	 */
	void completeIngestion(TinyflowDocument completedDocument, List<TinyflowChunk> chunks,
			TinyflowIngestionTask completedTask);

	/**
	 * Atomically completes ingestion and persists every compensation needed to clean the versions made
	 * inactive by the active-document switch. A failure in any write, including compensation persistence,
	 * must leave chunks, documents, active state, tasks, and compensations unchanged.
	 */
	void completeIngestionWithVectorCompensations(TinyflowDocument completedDocument, List<TinyflowChunk> chunks,
			TinyflowIngestionTask completedTask, List<VectorCompensation> compensations);

	List<TinyflowIngestionTask> findTasks(String tenantId, String knowledgeId);

	void saveVectorCompensation(VectorCompensation compensation);

	List<VectorCompensation> findVectorCompensations(String tenantId, String knowledgeId);

	/**
	 * Atomically advances a durable compensation stage. Repeating an already applied transition is a
	 * no-op; any other stale expected stage must fail without changing the record.
	 */
	void advanceVectorCompensationStage(String tenantId, String knowledgeId, String compensationId,
			String expectedStage, String nextStage);

	void deleteVectorCompensation(String tenantId, String knowledgeId, String compensationId);

	final class VectorCompensation {
		public static final int LEGACY_STAGE_SCHEMA_VERSION = 1;
		public static final int CURRENT_STAGE_SCHEMA_VERSION = 2;
		public static final String VECTOR_PENDING = "VECTOR_PENDING";
		public static final String OBJECT_PENDING = "OBJECT_PENDING";
		public static final String RESOURCE_PENDING = "RESOURCE_PENDING";
		public static final String TASK_PENDING = "TASK_PENDING";
		public static final String COMPLETE = "COMPLETE";

		private final String compensationId;
		private final String tenantId;
		private final String knowledgeId;
		private final String sourceId;
		private final String documentId;
		private final List<String> vectorIds;
		private final String operation;
		private final String stage;
		private final String objectKey;
		private final int stageSchemaVersion;

		public VectorCompensation(String compensationId, String tenantId, String knowledgeId,
				String sourceId, String documentId, List<String> vectorIds, String operation) {
			this(compensationId, tenantId, knowledgeId, sourceId, documentId, vectorIds,
					operation, VECTOR_PENDING, null, CURRENT_STAGE_SCHEMA_VERSION);
		}

		public VectorCompensation(String compensationId, String tenantId, String knowledgeId,
				String sourceId, String documentId, List<String> vectorIds, String operation, String stage) {
			this(compensationId, tenantId, knowledgeId, sourceId, documentId, vectorIds, operation, stage, null,
					LEGACY_STAGE_SCHEMA_VERSION);
		}

		public VectorCompensation(String compensationId, String tenantId, String knowledgeId,
				String sourceId, String documentId, List<String> vectorIds, String operation, String stage,
				String objectKey) {
			this(compensationId, tenantId, knowledgeId, sourceId, documentId, vectorIds, operation, stage, objectKey,
					CURRENT_STAGE_SCHEMA_VERSION);
		}

		public VectorCompensation(String compensationId, String tenantId, String knowledgeId,
				String sourceId, String documentId, List<String> vectorIds, String operation, String stage,
				String objectKey, int stageSchemaVersion) {
			this.compensationId = TinyflowKnowledgeReference.requireText(compensationId, "compensationId");
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.knowledgeId = TinyflowKnowledgeReference.requireText(knowledgeId, "knowledgeId");
			this.sourceId = TinyflowKnowledgeReference.requireText(sourceId, "sourceId");
			this.documentId = TinyflowKnowledgeReference.requireText(documentId, "documentId");
			this.vectorIds = vectorIds == null ? Collections.<String>emptyList()
					: Collections.unmodifiableList(new ArrayList<String>(vectorIds));
			this.operation = TinyflowKnowledgeReference.requireText(operation, "operation");
			this.stage = TinyflowKnowledgeReference.requireText(stage, "stage");
			this.objectKey = objectKey;
			if (stageSchemaVersion < LEGACY_STAGE_SCHEMA_VERSION) {
				throw new IllegalArgumentException("stageSchemaVersion must be positive");
			}
			this.stageSchemaVersion = stageSchemaVersion;
		}

		public String getCompensationId() { return compensationId; }
		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getSourceId() { return sourceId; }
		public String getDocumentId() { return documentId; }
		public List<String> getVectorIds() { return vectorIds; }
		public String getOperation() { return operation; }
		public String getStage() { return stage; }
		public String getObjectKey() { return objectKey; }
		public int getStageSchemaVersion() { return stageSchemaVersion; }

		public VectorCompensation withStage(String nextStage) {
			return new VectorCompensation(compensationId, tenantId, knowledgeId, sourceId, documentId,
					vectorIds, operation, nextStage, objectKey, stageSchemaVersion);
		}
	}

	final class OptimisticLockException extends BizException {
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

	final class VectorStoreOwnershipException extends BizException {
		private static final long serialVersionUID = 1L;
		private final String vectorStoreKey;

		public VectorStoreOwnershipException(String vectorStoreKey) {
			super("Vector store is already owned and does not support metadata filtering: " + vectorStoreKey);
			this.vectorStoreKey = TinyflowKnowledgeReference.requireText(vectorStoreKey, "vectorStoreKey");
		}

		public String getVectorStoreKey() { return vectorStoreKey; }
	}

	final class CompensationStageException extends BizException {
		private static final long serialVersionUID = 1L;
		private final String expectedStage;
		private final String actualStage;

		public CompensationStageException(String expectedStage, String actualStage) {
			super("Compensation stage conflict: expected " + expectedStage + " but was " + actualStage);
			this.expectedStage = expectedStage;
			this.actualStage = actualStage;
		}

		public String getExpectedStage() { return expectedStage; }
		public String getActualStage() { return actualStage; }
	}
}
