package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TinyflowKnowledgeRepositoryContractTest
		extends AbstractTinyflowKnowledgeRepositoryContractTest {

	@Override
	protected TinyflowKnowledgeRepository createRepository() {
		return new InMemoryTinyflowKnowledgeRepository();
	}

	private static final class InMemoryTinyflowKnowledgeRepository implements TinyflowKnowledgeRepository {

		private final Map<String, TinyflowKnowledgeDefinition> knowledge = new LinkedHashMap<String, TinyflowKnowledgeDefinition>();
		private final Map<String, Long> versions = new LinkedHashMap<String, Long>();
		private final Map<String, TinyflowDocument> documents = new LinkedHashMap<String, TinyflowDocument>();
		private final Map<String, String> activeDocuments = new LinkedHashMap<String, String>();
		private final Object documentVersionLock = new Object();
		private final Map<String, TinyflowChunk> chunks = new LinkedHashMap<String, TinyflowChunk>();
		private final List<TinyflowIngestionTask> tasks = new ArrayList<TinyflowIngestionTask>();
		private final List<VectorCompensation> compensations = new ArrayList<VectorCompensation>();

		@Override
		public TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId) {
			return knowledge.get(key(tenantId, knowledgeId));
		}

		@Override
		public long findKnowledgeVersion(String tenantId, String knowledgeId) {
			Long version = versions.get(key(tenantId, knowledgeId));
			return version == null ? 0L : version;
		}

		@Override
		public void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion) {
			String key = key(definition.getTenantId(), definition.getKnowledgeId());
			long actualVersion = findKnowledgeVersion(definition.getTenantId(), definition.getKnowledgeId());
			if (actualVersion != expectedVersion) {
				throw new OptimisticLockException(expectedVersion, actualVersion);
			}
			knowledge.put(key, definition);
			versions.put(key, actualVersion + 1L);
		}

		@Override
		public TinyflowDocument findBySourceAndChecksum(String tenantId, String knowledgeId,
				String sourceId, String checksum) {
			for (TinyflowDocument document : findDocumentVersions(tenantId, knowledgeId, sourceId)) {
				if (checksum.equals(document.getMetadata().get("checksum"))) {
					return document;
				}
			}
			return null;
		}

		@Override
		public TinyflowDocument findActiveDocument(String tenantId, String knowledgeId, String sourceId) {
			synchronized (documentVersionLock) {
				String documentId = activeDocuments.get(sourceKey(tenantId, knowledgeId, sourceId));
				return documentId == null ? null : documents.get(documentKey(tenantId, knowledgeId, documentId));
			}
		}

		@Override
		public void saveDocument(TinyflowDocument document) {
			synchronized (documentVersionLock) {
				documents.put(documentKey(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId()), document);
			}
		}

		@Override
		public List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId, String sourceId) {
			List<TinyflowDocument> result = new ArrayList<TinyflowDocument>();
			for (TinyflowDocument document : documents.values()) {
				if (tenantId.equals(document.getTenantId())
						&& knowledgeId.equals(document.getKnowledgeId())
						&& sourceId.equals(document.getSourceId())) {
					result.add(document);
				}
			}
			return result;
		}

		@Override
		public void activateDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId) {
			synchronized (documentVersionLock) {
				TinyflowDocument document = documents.get(documentKey(tenantId, knowledgeId, documentId));
				if (document == null || !sourceId.equals(document.getSourceId())) {
					throw new IllegalArgumentException("Document does not belong to source");
				}
				activeDocuments.put(sourceKey(tenantId, knowledgeId, sourceId), documentId);
			}
		}

		@Override
		public void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId) {
			for (TinyflowDocument document : new ArrayList<TinyflowDocument>(documents.values())) {
				if (tenantId.equals(document.getTenantId())
						&& knowledgeId.equals(document.getKnowledgeId())
						&& sourceId.equals(document.getSourceId())) {
					documents.remove(documentKey(tenantId, knowledgeId, document.getDocumentId()));
				}
			}
			activeDocuments.remove(sourceKey(tenantId, knowledgeId, sourceId));
		}

		@Override
		public void saveChunks(List<TinyflowChunk> values) {
			for (TinyflowChunk chunk : values) {
				chunks.put(chunkKey(chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getChunkId()), chunk);
			}
		}

		@Override
		public TinyflowChunk findChunk(String tenantId, String knowledgeId, String chunkId) {
			return chunks.get(chunkKey(tenantId, knowledgeId, chunkId));
		}

		@Override
		public List<TinyflowChunk> findChunksByDocument(String tenantId, String knowledgeId, String documentId) {
			List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
			for (TinyflowChunk chunk : chunks.values()) {
				if (tenantId.equals(chunk.getTenantId())
						&& knowledgeId.equals(chunk.getKnowledgeId())
						&& documentId.equals(chunk.getDocumentId())) {
					result.add(chunk);
				}
			}
			return result;
		}

		@Override
		public List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId, List<String> vectorIds) {
			List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
			for (TinyflowChunk chunk : chunks.values()) {
				if (tenantId.equals(chunk.getTenantId())
						&& knowledgeId.equals(chunk.getKnowledgeId())
						&& !Collections.disjoint(chunk.getVectorIds(), vectorIds)) {
					result.add(chunk);
				}
			}
			return result;
		}

		@Override
		public void updateChunk(String tenantId, String knowledgeId, String chunkId,
				boolean enabled, String content, Map<String, Object> metadata) {
			String key = chunkKey(tenantId, knowledgeId, chunkId);
			TinyflowChunk original = chunks.get(key);
			if (original == null) {
				throw new IllegalArgumentException("Chunk not found");
			}
			TinyflowChunk updated = TinyflowChunk.builder()
					.tenantId(original.getTenantId())
					.knowledgeId(original.getKnowledgeId())
					.sourceId(original.getSourceId())
					.sourceType(original.getSourceType())
					.documentId(original.getDocumentId())
					.chunkId(original.getChunkId())
					.content(content)
					.sourceUri(original.getSourceUri())
					.sourceVersion(original.getSourceVersion())
					.location(original.getLocation())
					.position(original.getPosition())
					.parentChunkId(original.getParentChunkId())
					.tokenCount(original.getTokenCount())
					.metadata(metadata)
					.enabled(enabled)
					.vectorIds(original.getVectorIds())
					.build();
			chunks.put(key, updated);
		}

		@Override
		public void saveTask(TinyflowIngestionTask task) {
			tasks.add(task);
		}

		@Override
		public List<TinyflowIngestionTask> findTasks(String tenantId, String knowledgeId) {
			List<TinyflowIngestionTask> result = new ArrayList<TinyflowIngestionTask>();
			for (TinyflowIngestionTask task : tasks) {
				if (tenantId.equals(task.getTenantId()) && knowledgeId.equals(task.getKnowledgeId())) {
					result.add(task);
				}
			}
			return result;
		}

		@Override
		public void saveVectorCompensation(VectorCompensation compensation) {
			compensations.add(compensation);
		}

		@Override
		public List<VectorCompensation> findVectorCompensations(String tenantId, String knowledgeId) {
			List<VectorCompensation> result = new ArrayList<VectorCompensation>();
			for (VectorCompensation compensation : compensations) {
				if (tenantId.equals(compensation.getTenantId())
						&& knowledgeId.equals(compensation.getKnowledgeId())) {
					result.add(compensation);
				}
			}
			return result;
		}

		@Override
		public void deleteVectorCompensation(String tenantId, String knowledgeId, String compensationId) {
			for (VectorCompensation compensation : new ArrayList<VectorCompensation>(compensations)) {
				if (tenantId.equals(compensation.getTenantId())
						&& knowledgeId.equals(compensation.getKnowledgeId())
						&& compensationId.equals(compensation.getCompensationId())) {
					compensations.remove(compensation);
				}
			}
		}

		private static String key(String tenantId, String knowledgeId) {
			return tenantId + "|" + knowledgeId;
		}

		private static String sourceKey(String tenantId, String knowledgeId, String sourceId) {
			return key(tenantId, knowledgeId) + "|" + sourceId;
		}

		private static String documentKey(String tenantId, String knowledgeId, String documentId) {
			return key(tenantId, knowledgeId) + "|" + documentId;
		}

		private static String chunkKey(String tenantId, String knowledgeId, String chunkId) {
			return key(tenantId, knowledgeId) + "|" + chunkId;
		}

	}
}
