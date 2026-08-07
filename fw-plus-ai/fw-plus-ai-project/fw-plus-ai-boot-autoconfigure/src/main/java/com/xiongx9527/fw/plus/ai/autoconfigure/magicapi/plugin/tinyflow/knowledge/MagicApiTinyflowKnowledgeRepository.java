package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowChunkMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDifyConnectionMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDocumentMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowIngestionTaskMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeKeyMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDifyConnectionRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class MagicApiTinyflowKnowledgeRepository implements TinyflowKnowledgeRepository,
		TinyflowKnowledgeAccessKeyRepository, TinyflowDifyConnectionRepository {

	private static final String TASK = "task";
	private final MagicResourceService resources;
	private final ReentrantLock lock = new ReentrantLock(true);
	private volatile Throwable rollbackFailure;
	private volatile String failCompletionAt;
	private volatile String failCompensationStartAt;
	private volatile String pauseCompletionAfter;
	private volatile CountDownLatch completionPaused = new CountDownLatch(1);
	private volatile CountDownLatch releaseCompletion = new CountDownLatch(1);
	private volatile CountDownLatch readerEntered;
	private volatile CountDownLatch writerEntered;
	private volatile Thread readerThread;
	private volatile Thread writerThread;
	private volatile int readerEntryCount;
	private volatile int writerEntryCount;

	public MagicApiTinyflowKnowledgeRepository(MagicResourceService resources) {
		if (resources == null) throw new IllegalArgumentException("MagicResourceService must not be null");
		this.resources = resources;
	}

	@Override public TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId) {
		return read(() -> {
			TinyflowKnowledgeMagicResource value = knowledgeResource(tenantId, knowledgeId);
			return value == null ? null : knowledge(value);
		});
	}

	@Override public long findKnowledgeVersion(String tenantId, String knowledgeId) {
		return read(() -> {
			TinyflowKnowledgeMagicResource value = knowledgeResource(tenantId, knowledgeId);
			return value == null ? 0L : value.getVersion();
		});
	}

	@Override public void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion) {
		write(() -> saveKnowledgeUnsafe(definition, expectedVersion, null, null));
	}

	@Override public void saveKnowledgeWithVectorStoreOwnership(TinyflowKnowledgeDefinition definition,
			long expectedVersion, String vectorStoreKey, boolean metadataFilterSupported) {
		write(() -> {
			required(vectorStoreKey, "vectorStoreKey");
			for (TinyflowKnowledgeMagicResource value : files(TinyflowKnowledgeMagicResourceStorage.FOLDER)) {
				if (same(value.getTenantId(), definition.getTenantId())
						&& same(value.getKnowledgeId(), definition.getKnowledgeId())) continue;
				if (same(vectorStoreKey, value.getVectorStoreKey())
						&& (!metadataFilterSupported || !Boolean.TRUE.equals(value.getMetadataFilterSupported()))) {
					throw new VectorStoreOwnershipException(vectorStoreKey);
				}
			}
			saveKnowledgeUnsafe(definition, expectedVersion, vectorStoreKey, metadataFilterSupported);
		});
	}

	@Override public void deleteKnowledge(String tenantId, String knowledgeId, long expectedVersion) {
		write(() -> {
			TinyflowKnowledgeMagicResource existing = knowledgeResource(tenantId, knowledgeId);
			long actual = existing == null ? 0L : existing.getVersion();
			if (actual != expectedVersion) throw new OptimisticLockException(expectedVersion, actual);
			if (existing != null) remove(existing);
		});
	}

	@Override public TinyflowDocument findBySourceAndChecksum(String tenantId, String knowledgeId,
			String sourceId, String checksum) {
		return read(() -> {
			for (TinyflowKnowledgeMagicResource value : documentResources(tenantId, knowledgeId, sourceId)) {
				if (same(checksum, value.getChecksum())) return document(value);
			}
			return null;
		});
	}

	@Override public TinyflowDocument findActiveDocument(String tenantId, String knowledgeId, String sourceId) {
		return read(() -> {
			for (TinyflowKnowledgeMagicResource value : documentResources(tenantId, knowledgeId, sourceId)) {
				if (value.isActive()) return document(value);
			}
			return null;
		});
	}

	@Override public void saveDocument(TinyflowDocument document) { write(() -> saveDocumentUnsafe(document)); }

	@Override public List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId, String sourceId) {
		return read(() -> {
			List<TinyflowDocument> result = new ArrayList<TinyflowDocument>();
			for (TinyflowKnowledgeMagicResource value : documentResources(tenantId, knowledgeId, sourceId)) result.add(document(value));
			return result;
		});
	}

	@Override public List<TinyflowDocument> findDocumentsByKnowledge(String tenantId, String knowledgeId) {
		return read(() -> {
			List<TinyflowDocument> result = new ArrayList<TinyflowDocument>();
			for (TinyflowKnowledgeMagicResource value : files(TinyflowDocumentMagicResourceStorage.FOLDER)) {
				if (scope(value, tenantId, knowledgeId)) result.add(document(value));
			}
			Collections.sort(result, new Comparator<TinyflowDocument>() {
				@Override public int compare(TinyflowDocument left, TinyflowDocument right) {
					int source = left.getSourceId().compareTo(right.getSourceId());
					return source != 0 ? source : left.getDocumentId().compareTo(right.getDocumentId());
				}
			});
			return result;
		});
	}

	@Override public void activateDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId) {
		write(() -> transaction(Collections.singletonList(TinyflowDocumentMagicResourceStorage.FOLDER), () ->
				activateDocumentVersionUnsafe(tenantId, knowledgeId, sourceId, documentId)));
	}

	@Override public void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId) {
		write(() -> {
			for (TinyflowKnowledgeMagicResource document : documentResources(tenantId, knowledgeId, sourceId)) remove(document);
			for (TinyflowKnowledgeMagicResource chunk : files(TinyflowChunkMagicResourceStorage.FOLDER)) {
				if (scope(chunk, tenantId, knowledgeId) && same(sourceId, chunk.getSourceId())) remove(chunk);
			}
		});
	}

	@Override public void deleteDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId) {
		write(() -> {
			TinyflowKnowledgeMagicResource target = documentResource(tenantId, knowledgeId, documentId);
			if (target == null) return;
			if (!same(sourceId, target.getSourceId())) throw new IllegalArgumentException("Document does not belong to source");
			if (target.isActive()) throw new IllegalStateException("Active document version cannot be deleted");
			remove(target);
			for (TinyflowKnowledgeMagicResource chunk : files(TinyflowChunkMagicResourceStorage.FOLDER)) {
				if (scope(chunk, tenantId, knowledgeId) && same(documentId, chunk.getDocumentId())) remove(chunk);
			}
		});
	}

	@Override public void saveChunks(List<TinyflowChunk> chunks) {
		write(() -> saveChunksUnsafe(chunks));
	}

	@Override public TinyflowChunk findChunk(String tenantId, String knowledgeId, String chunkId) {
		return read(() -> {
			TinyflowKnowledgeMagicResource value = chunkResource(tenantId, knowledgeId, chunkId);
			return value == null ? null : chunk(value);
		});
	}

	@Override public List<TinyflowChunk> findChunksByDocument(String tenantId, String knowledgeId, String documentId) {
		return read(() -> chunksByDocument(tenantId, knowledgeId, documentId));
	}

	@Override public List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId, List<String> vectorIds) {
		return read(() -> {
			List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
			List<String> ids = vectorIds == null ? Collections.<String>emptyList() : vectorIds;
			for (TinyflowKnowledgeMagicResource value : files(TinyflowChunkMagicResourceStorage.FOLDER)) {
				if (scope(value, tenantId, knowledgeId) && value.getVectorIds() != null
						&& !Collections.disjoint(value.getVectorIds(), ids)) result.add(chunk(value));
			}
			return result;
		});
	}

	@Override public List<TinyflowKnowledgeMatch> searchChunksByKeyword(String tenantId, String knowledgeId,
			String query, int limit) {
		return read(() -> {
			required(query, "query");
			if (limit <= 0) return Collections.emptyList();
			String term = query.toLowerCase(Locale.ROOT);
			List<TinyflowKnowledgeMatch> result = new ArrayList<TinyflowKnowledgeMatch>();
			for (TinyflowKnowledgeMagicResource value : files(TinyflowChunkMagicResourceStorage.FOLDER)) {
				if (scope(value, tenantId, knowledgeId) && value.isEnabled() && value.getContent() != null
						&& value.getContent().toLowerCase(Locale.ROOT).contains(term)) {
					result.add(match(value, 1D));
				}
			}
			Collections.sort(result, Comparator.comparing(TinyflowKnowledgeMatch::getChunkId));
			return result.size() <= limit ? result : new ArrayList<TinyflowKnowledgeMatch>(result.subList(0, limit));
		});
	}

	@Override public void updateChunk(String tenantId, String knowledgeId, String chunkId, boolean enabled,
			String content, Map<String, Object> metadata) {
		write(() -> {
			TinyflowKnowledgeMagicResource value = chunkResource(tenantId, knowledgeId, chunkId);
			if (value == null) throw new IllegalArgumentException("Chunk not found");
			value.setEnabled(enabled); value.setContent(content); value.setMetadata(copy(metadata)); save(value);
		});
	}

	@Override public void replaceChunkWithVectorCompensation(TinyflowChunk chunk,
			VectorCompensation compensation) {
		write(() -> transaction(Arrays.asList(TinyflowChunkMagicResourceStorage.FOLDER,
				TinyflowIngestionTaskMagicResourceStorage.FOLDER), () -> {
			if (compensation == null) throw new IllegalArgumentException("Vector compensation must not be null");
			TinyflowKnowledgeMagicResource existing = chunkResource(
					chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getChunkId());
			if (existing == null) throw new IllegalArgumentException("Chunk not found");
			if (!same(existing.getSourceId(), chunk.getSourceId())
					|| !same(existing.getDocumentId(), chunk.getDocumentId())) {
				throw new IllegalArgumentException("Chunk identity cannot be changed");
			}
			if (!scope(existing, compensation.getTenantId(), compensation.getKnowledgeId())
					|| !same(existing.getSourceId(), compensation.getSourceId())
					|| !same(existing.getDocumentId(), compensation.getDocumentId())) {
				throw new IllegalArgumentException("Vector compensation does not belong to chunk");
			}
			if (!copyList(existing.getVectorIds()).equals(compensation.getVectorIds())) {
				throw new IllegalStateException("Chunk vector mapping changed before replacement");
			}
			saveChunksUnsafe(Collections.singletonList(chunk));
			afterCompletionStep("chunk-replacement");
			saveCompensationUnsafe(compensation);
			afterCompletionStep("chunk-compensation");
		}));
	}

	@Override public void saveTask(TinyflowIngestionTask task) { write(() -> saveTaskUnsafe(task)); }

	@Override public void saveTaskWithVectorCompensation(TinyflowIngestionTask task, VectorCompensation compensation) {
		write(() -> transaction(Collections.singletonList(TinyflowIngestionTaskMagicResourceStorage.FOLDER), () -> {
			saveTaskUnsafe(task); afterCompensationStartStep("task");
			saveCompensationUnsafe(compensation); afterCompensationStartStep("compensation");
		}));
	}

	@Override public void completeIngestion(TinyflowDocument completedDocument, List<TinyflowChunk> chunks,
			TinyflowIngestionTask completedTask) {
		completeIngestionWithVectorCompensations(completedDocument, chunks, completedTask,
				Collections.<VectorCompensation>emptyList());
	}

	@Override public void completeIngestionWithVectorCompensations(TinyflowDocument completedDocument,
			List<TinyflowChunk> chunks, TinyflowIngestionTask completedTask, List<VectorCompensation> compensations) {
		write(() -> transaction(Arrays.asList(TinyflowChunkMagicResourceStorage.FOLDER,
				TinyflowDocumentMagicResourceStorage.FOLDER, TinyflowIngestionTaskMagicResourceStorage.FOLDER), () -> {
			saveChunksUnsafe(chunks); afterCompletionStep("chunks");
			saveDocumentUnsafe(completedDocument); afterCompletionStep("document");
			activateDocumentVersionUnsafe(completedDocument.getTenantId(), completedDocument.getKnowledgeId(),
					completedDocument.getSourceId(), completedDocument.getDocumentId()); afterCompletionStep("active");
			saveTaskUnsafe(completedTask); afterCompletionStep("task");
			for (VectorCompensation compensation : safe(compensations)) saveCompensationUnsafe(compensation);
			afterCompletionStep("compensation");
		}));
	}

	@Override public List<TinyflowIngestionTask> findTasks(String tenantId, String knowledgeId) {
		return read(() -> {
			List<TinyflowIngestionTask> result = new ArrayList<TinyflowIngestionTask>();
			for (TinyflowKnowledgeMagicResource value : files(TinyflowIngestionTaskMagicResourceStorage.FOLDER)) {
				if (scope(value, tenantId, knowledgeId) && !TinyflowIngestionTaskMagicResourceStorage.COMPENSATION.equals(value.getRecordType())) result.add(task(value));
			}
			return result;
		});
	}

	@Override public void saveVectorCompensation(VectorCompensation compensation) { write(() -> saveCompensationUnsafe(compensation)); }

	@Override public List<VectorCompensation> findVectorCompensations(String tenantId, String knowledgeId) {
		return read(() -> {
			List<VectorCompensation> result = new ArrayList<VectorCompensation>();
			for (TinyflowKnowledgeMagicResource value : files(TinyflowIngestionTaskMagicResourceStorage.FOLDER)) {
				if (scope(value, tenantId, knowledgeId) && TinyflowIngestionTaskMagicResourceStorage.COMPENSATION.equals(value.getRecordType())) result.add(compensation(value));
			}
			return result;
		});
	}

	@Override public void advanceVectorCompensationStage(String tenantId, String knowledgeId,
			String compensationId, String expectedStage, String nextStage) {
		write(() -> {
			TinyflowKnowledgeMagicResource value = compensationResource(tenantId, knowledgeId, compensationId);
			String actual = value == null ? null : value.getStage();
			if (value != null && same(nextStage, actual)) return;
			if (value == null || !same(expectedStage, actual)) throw new CompensationStageException(expectedStage, actual);
			value.setStage(nextStage); save(value);
		});
	}

	@Override public void deleteVectorCompensation(String tenantId, String knowledgeId, String compensationId) {
		write(() -> {
			TinyflowKnowledgeMagicResource value = compensationResource(tenantId, knowledgeId, compensationId);
			if (value != null) remove(value);
		});
	}

	@Override public AccessKey findByHash(String accessKeyHash) {
		return read(() -> uniqueAccessKey(value -> same(accessKeyHash, value.getAccessKeyHash())));
	}

	@Override public AccessKey findByPrefix(String prefix) {
		return read(() -> uniqueAccessKey(value -> same(prefix, value.getPrefix())));
	}

	@Override public void save(AccessKey accessKey) {
		write(() -> {
			for (TinyflowKnowledgeMagicResource value : files(TinyflowKnowledgeKeyMagicResourceStorage.FOLDER)) {
				if (same(accessKey.getPrefix(), value.getPrefix()) && !same(accessKey.getAccessKeyId(), value.getAccessKeyId())) {
					throw new IllegalArgumentException("Access key prefix already exists");
				}
			}
			TinyflowKnowledgeMagicResource value = find(files(TinyflowKnowledgeKeyMagicResourceStorage.FOLDER),
					item -> same(accessKey.getAccessKeyId(), item.getAccessKeyId()));
			value = prepare(value, TinyflowKnowledgeKeyMagicResourceStorage.FOLDER);
			value.setRecordType("access-key"); value.setTenantId(accessKey.getTenantId());
			value.setKnowledgeId(accessKey.getKnowledgeId()); value.setAccessKeyId(accessKey.getAccessKeyId());
			value.setPrefix(accessKey.getPrefix()); value.setHashAlgorithm(accessKey.getHashAlgorithm());
			value.setSalt(accessKey.getSalt()); value.setCost(accessKey.getCost());
			value.setAccessKeyHash(accessKey.getAccessKeyHash()); value.setEnabled(accessKey.isEnabled()); save(value);
		});
	}

	@Override public void delete(String tenantId, String knowledgeId, String accessKeyId) {
		write(() -> {
			TinyflowKnowledgeMagicResource value = find(files(TinyflowKnowledgeKeyMagicResourceStorage.FOLDER),
					item -> scope(item, tenantId, knowledgeId) && same(accessKeyId, item.getAccessKeyId()));
			if (value != null) remove(value);
		});
	}

	public List<AccessKey> findAccessKeys(String tenantId, String knowledgeId) {
		return read(() -> {
			List<AccessKey> result = new ArrayList<AccessKey>();
			for (TinyflowKnowledgeMagicResource value : files(TinyflowKnowledgeKeyMagicResourceStorage.FOLDER)) {
				if (scope(value, tenantId, knowledgeId)) result.add(accessKey(value));
			}
			return result;
		});
	}

	@Override public DifyConnection find(String tenantId, String connectionId) {
		return read(() -> {
			TinyflowKnowledgeMagicResource value = find(files(TinyflowDifyConnectionMagicResourceStorage.FOLDER),
					item -> same(tenantId, item.getTenantId()) && same(connectionId, item.getConnectionId()));
			return value == null ? null : connection(value);
		});
	}

	@Override public List<DifyConnection> findByKnowledge(String tenantId, String knowledgeId) {
		return read(() -> {
			List<DifyConnection> result = new ArrayList<DifyConnection>();
			for (TinyflowKnowledgeMagicResource value : files(TinyflowDifyConnectionMagicResourceStorage.FOLDER)) {
				if (scope(value, tenantId, knowledgeId)) result.add(connection(value));
			}
			return result;
		});
	}

	@Override public void save(DifyConnection connection) {
		write(() -> {
			TinyflowKnowledgeMagicResource value = find(files(TinyflowDifyConnectionMagicResourceStorage.FOLDER),
					item -> same(connection.getTenantId(), item.getTenantId()) && same(connection.getConnectionId(), item.getConnectionId()));
			value = prepare(value, TinyflowDifyConnectionMagicResourceStorage.FOLDER);
			value.setRecordType("dify-connection"); value.setTenantId(connection.getTenantId());
			value.setKnowledgeId(connection.getKnowledgeId()); value.setConnectionId(connection.getConnectionId());
			value.setBaseUrl(connection.getBaseUrl()); value.setDatasetId(connection.getDatasetId());
			value.setCredentialRef(connection.getCredentialRef()); save(value);
		});
	}

	@Override public void delete(String tenantId, String connectionId) {
		write(() -> {
			TinyflowKnowledgeMagicResource value = find(files(TinyflowDifyConnectionMagicResourceStorage.FOLDER),
					item -> same(tenantId, item.getTenantId()) && same(connectionId, item.getConnectionId()));
			if (value != null) remove(value);
		});
	}

	public List<TinyflowKnowledgeDefinition> findKnowledgeByTenant(String tenantId) {
		return read(() -> {
			List<TinyflowKnowledgeDefinition> result = new ArrayList<TinyflowKnowledgeDefinition>();
			for (TinyflowKnowledgeMagicResource value : files(TinyflowKnowledgeMagicResourceStorage.FOLDER)) if (same(tenantId, value.getTenantId())) result.add(knowledge(value));
			return result;
		});
	}

	public TinyflowIngestionTask findTask(String tenantId, String knowledgeId, String taskId) {
		return read(() -> {
			TinyflowKnowledgeMagicResource value = taskResource(tenantId, knowledgeId, taskId);
			return value == null ? null : task(value);
		});
	}

	private void saveKnowledgeUnsafe(TinyflowKnowledgeDefinition definition, long expectedVersion,
			String vectorStoreKey, Boolean metadataFilterSupported) {
		if (definition == null) throw new IllegalArgumentException("definition must not be null");
		TinyflowKnowledgeMagicResource existing = knowledgeResource(definition.getTenantId(), definition.getKnowledgeId());
		long actual = existing == null ? 0L : existing.getVersion();
		if (actual != expectedVersion) throw new OptimisticLockException(expectedVersion, actual);
		TinyflowKnowledgeMagicResource value = prepare(existing, TinyflowKnowledgeMagicResourceStorage.FOLDER);
		value.setRecordType("knowledge"); value.setTenantId(definition.getTenantId()); value.setKnowledgeId(definition.getKnowledgeId());
		value.setProviderKey(definition.getProviderKey()); value.setDisplayName(definition.getName()); value.setDescription(definition.getDescription());
		value.setMetadata(copy(definition.getMetadata())); value.setEnabled(definition.isEnabled()); value.setVersion(actual + 1L);
		if (vectorStoreKey != null) { value.setVectorStoreKey(vectorStoreKey); value.setMetadataFilterSupported(metadataFilterSupported); }
		save(value);
	}

	private void saveDocumentUnsafe(TinyflowDocument document) {
		if (document == null) throw new IllegalArgumentException("document must not be null");
		TinyflowKnowledgeMagicResource existing = documentResource(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId());
		TinyflowKnowledgeMagicResource value = prepare(existing, TinyflowDocumentMagicResourceStorage.FOLDER);
		value.setRecordType("document"); value.setTenantId(document.getTenantId()); value.setKnowledgeId(document.getKnowledgeId());
		value.setSourceId(document.getSourceId()); value.setSourceType(document.getSourceType()); value.setDocumentId(document.getDocumentId());
		value.setChecksum(document.getChecksum()); value.setDisplayName(document.getName() == null ? document.getDocumentId() : document.getName());
		value.setContent(document.getContent()); value.setSourceUri(document.getSourceUri()); value.setSourceVersion(document.getSourceVersion());
		value.setMetadata(copy(document.getMetadata())); value.setEnabled(document.isEnabled());
		value.setActive(existing != null && existing.isActive()); save(value);
	}

	private void saveChunksUnsafe(List<TinyflowChunk> chunks) {
		for (TinyflowChunk chunk : safe(chunks)) {
			TinyflowKnowledgeMagicResource existing = chunkResource(chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getChunkId());
			TinyflowKnowledgeMagicResource value = prepare(existing, TinyflowChunkMagicResourceStorage.FOLDER);
			value.setRecordType("chunk"); value.setTenantId(chunk.getTenantId()); value.setKnowledgeId(chunk.getKnowledgeId());
			value.setSourceId(chunk.getSourceId()); value.setSourceType(chunk.getSourceType()); value.setDocumentId(chunk.getDocumentId());
			value.setChunkId(chunk.getChunkId()); value.setContent(chunk.getContent()); value.setSourceUri(chunk.getSourceUri());
			value.setSourceVersion(chunk.getSourceVersion()); value.setLocation(chunk.getLocation()); value.setPosition(chunk.getPosition());
			value.setParentChunkId(chunk.getParentChunkId()); value.setTokenCount(chunk.getTokenCount()); value.setMetadata(copy(chunk.getMetadata()));
			value.setEnabled(chunk.isEnabled()); value.setVectorIds(copyList(chunk.getVectorIds())); save(value);
		}
	}

	private void saveTaskUnsafe(TinyflowIngestionTask task) {
		TinyflowKnowledgeMagicResource existing = taskResource(task.getTenantId(), task.getKnowledgeId(), task.getTaskId());
		TinyflowKnowledgeMagicResource value = prepare(existing, TinyflowIngestionTaskMagicResourceStorage.FOLDER);
		value.setRecordType(TASK); value.setTenantId(task.getTenantId()); value.setKnowledgeId(task.getKnowledgeId());
		value.setTaskId(task.getTaskId()); value.setProviderKey(task.getProviderKey()); value.setDocumentId(task.getDocumentId());
		value.setStatus(task.getStatus()); value.setMessage(task.getMessage()); value.setMetadata(copy(task.getMetadata())); save(value);
	}

	private void saveCompensationUnsafe(VectorCompensation compensation) {
		TinyflowKnowledgeMagicResource existing = compensationResource(compensation.getTenantId(), compensation.getKnowledgeId(), compensation.getCompensationId());
		TinyflowKnowledgeMagicResource value = prepare(existing, TinyflowIngestionTaskMagicResourceStorage.FOLDER);
		value.setRecordType(TinyflowIngestionTaskMagicResourceStorage.COMPENSATION); value.setTenantId(compensation.getTenantId());
		value.setKnowledgeId(compensation.getKnowledgeId()); value.setCompensationId(compensation.getCompensationId());
		value.setSourceId(compensation.getSourceId()); value.setDocumentId(compensation.getDocumentId());
		value.setVectorIds(copyList(compensation.getVectorIds())); value.setOperation(compensation.getOperation());
		value.setStage(compensation.getStage()); value.setObjectKey(compensation.getObjectKey());
		value.setStageSchemaVersion(compensation.getStageSchemaVersion());
		save(value);
	}

	private void activateDocumentVersionUnsafe(String tenantId, String knowledgeId, String sourceId, String documentId) {
		List<TinyflowKnowledgeMagicResource> versions = documentResources(tenantId, knowledgeId, sourceId);
		TinyflowKnowledgeMagicResource target = null;
		for (TinyflowKnowledgeMagicResource value : versions) if (same(documentId, value.getDocumentId())) target = value;
		if (target == null) throw new IllegalArgumentException("Document does not belong to source");
		for (TinyflowKnowledgeMagicResource value : versions) {
			boolean active = same(documentId, value.getDocumentId());
			if (value.isActive() != active) { value.setActive(active); save(value); }
		}
	}

	private TinyflowKnowledgeDefinition knowledge(TinyflowKnowledgeMagicResource value) {
		return TinyflowKnowledgeDefinition.builder().providerKey(value.getProviderKey()).tenantId(value.getTenantId())
				.knowledgeId(value.getKnowledgeId()).name(displayName(value)).description(value.getDescription())
				.metadata(copy(value.getMetadata())).enabled(value.isEnabled()).build();
	}

	private TinyflowDocument document(TinyflowKnowledgeMagicResource value) {
		return TinyflowDocument.builder().tenantId(value.getTenantId()).knowledgeId(value.getKnowledgeId())
				.sourceId(value.getSourceId()).sourceType(value.getSourceType()).documentId(value.getDocumentId())
				.checksum(value.getChecksum()).name(displayName(value)).content(value.getContent()).sourceUri(value.getSourceUri())
				.sourceVersion(value.getSourceVersion()).metadata(copy(value.getMetadata()))
				.chunks(chunksByDocument(value.getTenantId(), value.getKnowledgeId(), value.getDocumentId()))
				.enabled(value.isEnabled()).build();
	}

	private TinyflowChunk chunk(TinyflowKnowledgeMagicResource value) {
		return TinyflowChunk.builder().tenantId(value.getTenantId()).knowledgeId(value.getKnowledgeId())
				.sourceId(value.getSourceId()).sourceType(value.getSourceType()).documentId(value.getDocumentId())
				.chunkId(value.getChunkId()).content(value.getContent()).sourceUri(value.getSourceUri())
				.sourceVersion(value.getSourceVersion()).location(value.getLocation()).position(value.getPosition())
				.parentChunkId(value.getParentChunkId()).tokenCount(value.getTokenCount()).metadata(copy(value.getMetadata()))
				.enabled(value.isEnabled()).vectorIds(copyList(value.getVectorIds())).build();
	}

	private TinyflowKnowledgeMatch match(TinyflowKnowledgeMagicResource value, double score) {
		return TinyflowKnowledgeMatch.builder().tenantId(value.getTenantId()).knowledgeId(value.getKnowledgeId())
				.sourceId(value.getSourceId()).sourceType(value.getSourceType()).documentId(value.getDocumentId())
				.chunkId(value.getChunkId()).content(value.getContent()).sourceUri(value.getSourceUri())
				.sourceVersion(value.getSourceVersion()).location(value.getLocation()).position(value.getPosition())
				.parentChunkId(value.getParentChunkId()).tokenCount(value.getTokenCount()).metadata(copy(value.getMetadata()))
				.enabled(value.isEnabled()).vectorIds(copyList(value.getVectorIds())).score(score).build();
	}

	private TinyflowIngestionTask task(TinyflowKnowledgeMagicResource value) {
		return TinyflowIngestionTask.builder().taskId(value.getTaskId()).providerKey(value.getProviderKey())
				.tenantId(value.getTenantId()).knowledgeId(value.getKnowledgeId()).documentId(value.getDocumentId())
				.status(value.getStatus()).message(value.getMessage()).metadata(copy(value.getMetadata())).build();
	}

	private VectorCompensation compensation(TinyflowKnowledgeMagicResource value) {
		int stageSchemaVersion = value.getStageSchemaVersion() == null
				? VectorCompensation.LEGACY_STAGE_SCHEMA_VERSION : value.getStageSchemaVersion();
		return new VectorCompensation(value.getCompensationId(), value.getTenantId(), value.getKnowledgeId(),
				value.getSourceId(), value.getDocumentId(), copyList(value.getVectorIds()), value.getOperation(),
				value.getStage(), value.getObjectKey(), stageSchemaVersion);
	}

	private AccessKey accessKey(TinyflowKnowledgeMagicResource value) {
		return new AccessKey(value.getAccessKeyId(), value.getTenantId(), value.getKnowledgeId(), value.getPrefix(),
				value.getHashAlgorithm(), value.getSalt(), value.getCost(), value.getAccessKeyHash(), value.isEnabled());
	}

	private DifyConnection connection(TinyflowKnowledgeMagicResource value) {
		return new DifyConnection(value.getConnectionId(), value.getTenantId(), value.getKnowledgeId(),
				value.getBaseUrl(), value.getDatasetId(), value.getCredentialRef());
	}

	private TinyflowKnowledgeMagicResource knowledgeResource(String tenantId, String knowledgeId) {
		return find(files(TinyflowKnowledgeMagicResourceStorage.FOLDER), value -> scope(value, tenantId, knowledgeId));
	}

	private TinyflowKnowledgeMagicResource documentResource(String tenantId, String knowledgeId, String documentId) {
		return find(files(TinyflowDocumentMagicResourceStorage.FOLDER), value -> scope(value, tenantId, knowledgeId) && same(documentId, value.getDocumentId()));
	}

	private List<TinyflowKnowledgeMagicResource> documentResources(String tenantId, String knowledgeId, String sourceId) {
		List<TinyflowKnowledgeMagicResource> result = new ArrayList<TinyflowKnowledgeMagicResource>();
		for (TinyflowKnowledgeMagicResource value : files(TinyflowDocumentMagicResourceStorage.FOLDER)) if (scope(value, tenantId, knowledgeId) && same(sourceId, value.getSourceId())) result.add(value);
		return result;
	}

	private TinyflowKnowledgeMagicResource chunkResource(String tenantId, String knowledgeId, String chunkId) {
		return find(files(TinyflowChunkMagicResourceStorage.FOLDER), value -> scope(value, tenantId, knowledgeId) && same(chunkId, value.getChunkId()));
	}

	private TinyflowKnowledgeMagicResource taskResource(String tenantId, String knowledgeId, String taskId) {
		return find(files(TinyflowIngestionTaskMagicResourceStorage.FOLDER), value -> scope(value, tenantId, knowledgeId)
				&& TASK.equals(value.getRecordType()) && same(taskId, value.getTaskId()));
	}

	private TinyflowKnowledgeMagicResource compensationResource(String tenantId, String knowledgeId, String compensationId) {
		return find(files(TinyflowIngestionTaskMagicResourceStorage.FOLDER), value -> scope(value, tenantId, knowledgeId)
				&& TinyflowIngestionTaskMagicResourceStorage.COMPENSATION.equals(value.getRecordType())
				&& same(compensationId, value.getCompensationId()));
	}

	private List<TinyflowChunk> chunksByDocument(String tenantId, String knowledgeId, String documentId) {
		List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
		for (TinyflowKnowledgeMagicResource value : files(TinyflowChunkMagicResourceStorage.FOLDER)) if (scope(value, tenantId, knowledgeId) && same(documentId, value.getDocumentId())) result.add(chunk(value));
		return result;
	}

	private AccessKey uniqueAccessKey(Predicate predicate) {
		TinyflowKnowledgeMagicResource match = null;
		for (TinyflowKnowledgeMagicResource value : files(TinyflowKnowledgeKeyMagicResourceStorage.FOLDER)) {
			if (!predicate.matches(value)) continue;
			if (match != null) throw new IllegalStateException("Access key lookup is ambiguous");
			match = value;
		}
		return match == null ? null : accessKey(match);
	}

	private TinyflowKnowledgeMagicResource prepare(TinyflowKnowledgeMagicResource existing, String folder) {
		if (existing != null) return existing.copyResource();
		TinyflowKnowledgeMagicResource value = new TinyflowKnowledgeMagicResource();
		String id = UUID.randomUUID().toString().replace("-", "");
		value.setId(id); value.setName(id); value.setGroupId(folder + ":0"); return value;
	}

	private void save(TinyflowKnowledgeMagicResource value) {
		value.setName(TinyflowKnowledgeResourceNameCodec.encode(Arrays.asList(value.getRecordType(),
				value.getTenantId(), value.getKnowledgeId(), storageIdentity(value))));
		if (!resources.saveFile(value)) throw new IllegalStateException("Magic resource save failed");
	}

	private String storageIdentity(TinyflowKnowledgeMagicResource value) {
		if ("knowledge".equals(value.getRecordType())) return value.getKnowledgeId();
		if ("document".equals(value.getRecordType())) return value.getDocumentId();
		if ("chunk".equals(value.getRecordType())) return value.getChunkId();
		if (TASK.equals(value.getRecordType())) return value.getTaskId();
		if (TinyflowIngestionTaskMagicResourceStorage.COMPENSATION.equals(value.getRecordType())) return value.getCompensationId();
		if ("access-key".equals(value.getRecordType())) return value.getAccessKeyId();
		if ("dify-connection".equals(value.getRecordType())) return value.getConnectionId();
		throw new IllegalArgumentException("Unsupported Tinyflow knowledge record type: " + value.getRecordType());
	}

	private String displayName(TinyflowKnowledgeMagicResource value) {
		return value.getDisplayName() == null ? value.getName() : value.getDisplayName();
	}

	private void remove(TinyflowKnowledgeMagicResource value) {
		if (value != null && !resources.delete(value.getId())) throw new IllegalStateException("Magic resource delete failed");
	}

	@SuppressWarnings("unchecked")
	private List<TinyflowKnowledgeMagicResource> files(String folder) {
		List<TinyflowKnowledgeMagicResource> values = resources.files(folder);
		return values == null ? Collections.<TinyflowKnowledgeMagicResource>emptyList() : values;
	}

	private void transaction(List<String> folders, Mutation mutation) {
		Map<String, List<TinyflowKnowledgeMagicResource>> snapshot = snapshot(folders);
		try { mutation.run(); }
		catch (RuntimeException original) {
			try { restore(snapshot); }
			catch (RuntimeException rollback) { rollbackFailure = rollback; original.addSuppressed(rollback); }
			throw original;
		}
	}

	private Map<String, List<TinyflowKnowledgeMagicResource>> snapshot(List<String> folders) {
		Map<String, List<TinyflowKnowledgeMagicResource>> result = new LinkedHashMap<String, List<TinyflowKnowledgeMagicResource>>();
		for (String folder : new LinkedHashSet<String>(folders)) {
			List<TinyflowKnowledgeMagicResource> copies = new ArrayList<TinyflowKnowledgeMagicResource>();
			for (TinyflowKnowledgeMagicResource value : files(folder)) copies.add(value.copyResource());
			result.put(folder, copies);
		}
		return result;
	}

	private void restore(Map<String, List<TinyflowKnowledgeMagicResource>> snapshot) {
		for (String folder : snapshot.keySet()) for (TinyflowKnowledgeMagicResource value : new ArrayList<TinyflowKnowledgeMagicResource>(files(folder))) remove(value);
		for (List<TinyflowKnowledgeMagicResource> values : snapshot.values()) for (TinyflowKnowledgeMagicResource value : values) save(value.copyResource());
	}

	private <T> T read(Operation<T> operation) {
		recordReaderEntry(); lock.lock();
		try { ensureConsistent(); return operation.run(); }
		finally { lock.unlock(); }
	}

	private void write(Mutation mutation) {
		recordWriterEntry(); lock.lock();
		try { ensureConsistent(); mutation.run(); }
		finally { lock.unlock(); }
	}

	private void ensureConsistent() {
		if (rollbackFailure != null) throw new IllegalStateException("Magic knowledge repository rollback failed", rollbackFailure);
	}

	void failNextCompletionAt(String step) { failCompletionAt = step; }
	void failNextCompensationStartAt(String step) { failCompensationStartAt = step; }
	void pauseNextCompletionAfter(String step) {
		pauseCompletionAfter = step; completionPaused = new CountDownLatch(1); releaseCompletion = new CountDownLatch(1);
	}
	void awaitCompletionPaused() throws InterruptedException { await(completionPaused, "Completion did not pause"); }
	void releaseCompletion() { releaseCompletion.countDown(); }
	void expectReaderEntry() { readerThread = null; readerEntryCount = 0; readerEntered = new CountDownLatch(1); }
	void awaitReaderEntered() throws InterruptedException { await(readerEntered, "Reader did not enter"); }
	void awaitReaderBlocked() { awaitBlocked(readerThread, true); }
	void expectWriterEntry() { writerThread = null; writerEntryCount = 0; writerEntered = new CountDownLatch(1); }
	void awaitWriterEntered() throws InterruptedException { await(writerEntered, "Writer did not enter"); }
	void awaitWriterBlocked() { awaitBlocked(writerThread, false); }

	private void afterCompletionStep(String step) {
		if (same(step, failCompletionAt)) { failCompletionAt = null; throw new IllegalStateException("Completion failed after " + step); }
		if (same(step, pauseCompletionAfter)) {
			completionPaused.countDown();
			try { if (!releaseCompletion.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Completion pause timed out"); }
			catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException("Completion pause interrupted", exception); }
			pauseCompletionAfter = null;
		}
	}

	private void afterCompensationStartStep(String step) {
		if (same(step, failCompensationStartAt)) { failCompensationStartAt = null; throw new IllegalStateException("Compensation start failed after " + step); }
	}

	private void recordReaderEntry() {
		CountDownLatch entered = readerEntered;
		if (entered != null && readerThread == null) { readerThread = Thread.currentThread(); readerEntryCount = 1; entered.countDown(); }
		else if (entered != null && readerThread == Thread.currentThread()) readerEntryCount++;
	}

	private void recordWriterEntry() {
		CountDownLatch entered = writerEntered;
		if (entered != null && writerThread == null) { writerThread = Thread.currentThread(); writerEntryCount = 1; entered.countDown(); }
		else if (entered != null && writerThread == Thread.currentThread()) writerEntryCount++;
	}

	private void awaitBlocked(Thread thread, boolean reader) {
		if (thread == null) throw new AssertionError("Repository entry was not recorded");
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L);
		while (!lock.hasQueuedThread(thread)) {
			if ((reader ? readerEntryCount : writerEntryCount) != 1) throw new AssertionError("Repository path re-entered before blocking");
			if (!thread.isAlive()) throw new AssertionError("Repository operation returned before blocking");
			if (System.nanoTime() >= deadline) throw new AssertionError("Repository operation did not block");
			Thread.yield();
		}
	}

	private void await(CountDownLatch latch, String message) throws InterruptedException {
		if (latch == null || !latch.await(5, TimeUnit.SECONDS)) throw new AssertionError(message);
	}

	private boolean scope(TinyflowKnowledgeMagicResource value, String tenantId, String knowledgeId) {
		return same(tenantId, value.getTenantId()) && same(knowledgeId, value.getKnowledgeId());
	}
	private boolean same(Object left, Object right) { return left == null ? right == null : left.equals(right); }
	private void required(String value, String name) { if (value == null || value.trim().length() == 0) throw new IllegalArgumentException(name + " must not be blank"); }
	private Map<String, Object> copy(Map<String, Object> value) { return value == null ? Collections.<String, Object>emptyMap() : new LinkedHashMap<String, Object>(value); }
	private List<String> copyList(List<String> value) { return value == null ? Collections.<String>emptyList() : new ArrayList<String>(value); }
	private <T> List<T> safe(List<T> value) { return value == null ? Collections.<T>emptyList() : value; }
	private TinyflowKnowledgeMagicResource find(List<TinyflowKnowledgeMagicResource> values, Predicate predicate) {
		for (TinyflowKnowledgeMagicResource value : values) if (predicate.matches(value)) return value;
		return null;
	}

	private interface Predicate { boolean matches(TinyflowKnowledgeMagicResource value); }
	private interface Mutation { void run(); }
	private interface Operation<T> { T run(); }
}
