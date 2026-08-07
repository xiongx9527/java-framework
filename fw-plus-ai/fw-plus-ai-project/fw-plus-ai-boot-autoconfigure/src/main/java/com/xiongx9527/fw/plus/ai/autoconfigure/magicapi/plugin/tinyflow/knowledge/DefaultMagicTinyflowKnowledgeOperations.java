package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentImport;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.local.DefaultTinyflowIngestionService;
import com.xiongx9527.fw.plus.ai.tinyflow.local.index.TinyflowKnowledgeMaintenanceService;
import com.xiongx9527.fw.plus.ai.tinyflow.local.index.TinyflowVectorCompensator;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DefaultMagicTinyflowKnowledgeOperations implements MagicTinyflowKnowledgeOperations {

	private static final String LOCAL = "fw-plus-local";
	private static final String EMBEDDING_MODEL_KEY = "embeddingModelKey";
	private static final String VECTOR_STORE_KEY = "vectorStoreKey";
	private static final String S3_KEY = "s3Key";
	private static final Set<String> KNOWLEDGE_METADATA_KEYS = Collections.unmodifiableSet(
			new HashSet<String>(Arrays.asList(EMBEDDING_MODEL_KEY, VECTOR_STORE_KEY, S3_KEY)));
	private final MagicApiTinyflowKnowledgeRepository repository;
	private final TinyflowObjectStorage objectStorage;
	private final IngestionGateway ingestion;
	private final TinyflowKnowledgeProviderRegistry providers;
	private final TinyflowVectorStoreProvider vectorStores;
	private final TinyflowKnowledgeMaintenanceService maintenance;
	private final MagicTinyflowKnowledgeOptionProvider optionProvider;

	public DefaultMagicTinyflowKnowledgeOperations(MagicApiTinyflowKnowledgeRepository repository,
			TinyflowObjectStorage objectStorage, final DefaultTinyflowIngestionService ingestionService,
			TinyflowKnowledgeProviderRegistry providers, TinyflowVectorStoreProvider vectorStores,
			TinyflowKnowledgeMaintenanceService maintenance, MagicTinyflowKnowledgeOptionProvider optionProvider) {
		this(repository, objectStorage, new IngestionGateway() {
			@Override public TinyflowIngestionTask importDocument(TinyflowDocumentImport request) {
				return ingestionService.importDocument(request);
			}
			@Override public TinyflowIngestionTask cancel(String tenantId, String knowledgeId, String taskId) {
				return ingestionService.cancel(tenantId, knowledgeId, taskId);
			}
		}, providers, vectorStores, maintenance, optionProvider);
		if (ingestionService == null) throw new IllegalArgumentException("ingestionService must not be null");
	}

	DefaultMagicTinyflowKnowledgeOperations(MagicApiTinyflowKnowledgeRepository repository,
			TinyflowObjectStorage objectStorage, IngestionGateway ingestion,
			TinyflowKnowledgeProviderRegistry providers, TinyflowVectorStoreProvider vectorStores,
			TinyflowKnowledgeMaintenanceService maintenance, MagicTinyflowKnowledgeOptionProvider optionProvider) {
		if (repository == null || objectStorage == null || ingestion == null || providers == null
				|| vectorStores == null || maintenance == null || optionProvider == null) {
			throw new IllegalArgumentException("Knowledge operation dependencies must not be null");
		}
		this.repository = repository;
		this.objectStorage = objectStorage;
		this.ingestion = ingestion;
		this.providers = providers;
		this.vectorStores = vectorStores;
		this.maintenance = maintenance;
		this.optionProvider = optionProvider;
	}

	@Override public KnowledgeRecord create(TinyflowKnowledgeDefinition definition) {
		validateDefinition(definition);
		if (repository.findKnowledge(definition.getTenantId(), definition.getKnowledgeId()) != null) {
			throw new IllegalArgumentException("Knowledge already exists");
		}
		save(definition, 0L);
		return record(definition.getTenantId(), definition.getKnowledgeId());
	}

	@Override public KnowledgeRecord update(TinyflowKnowledgeDefinition definition, long expectedVersion) {
		validateDefinition(definition);
		if (expectedVersion <= 0L) throw new IllegalArgumentException("expectedVersion must be positive");
		save(definition, expectedVersion);
		return record(definition.getTenantId(), definition.getKnowledgeId());
	}

	@Override public KnowledgeRecord find(String tenantId, String knowledgeId) {
		TinyflowKnowledgeDefinition definition = repository.findKnowledge(tenantId, knowledgeId);
		return definition == null ? null : new KnowledgeRecord(definition,
				repository.findKnowledgeVersion(tenantId, knowledgeId));
	}

	@Override public PageResult<KnowledgeRecord> listKnowledge(String tenantId, int page, int size) {
		List<TinyflowKnowledgeDefinition> definitions = repository.findKnowledgeByTenant(tenantId);
		Collections.sort(definitions, Comparator.comparing(TinyflowKnowledgeDefinition::getKnowledgeId));
		List<KnowledgeRecord> records = new ArrayList<KnowledgeRecord>();
		for (TinyflowKnowledgeDefinition definition : definitions) {
			records.add(new KnowledgeRecord(definition,
					repository.findKnowledgeVersion(tenantId, definition.getKnowledgeId())));
		}
		return page(records, page, size);
	}

	@Override public void delete(String tenantId, String knowledgeId, long expectedVersion) {
		maintenance.deleteKnowledge(tenantId, knowledgeId, expectedVersion);
	}

	@Override public TinyflowIngestionTask importDocument(ImportCommand command) {
		if (command == null) throw new IllegalArgumentException("command must not be null");
		TinyflowKnowledgeDefinition knowledge = requireKnowledge(command.getTenantId(), command.getKnowledgeId());
		if (!knowledge.isEnabled()) throw new IllegalStateException("Knowledge is disabled");
		byte[] content = command.getContent();
		String checksum = sha256(content);
		TinyflowDocument active = repository.findActiveDocument(
				command.getTenantId(), command.getKnowledgeId(), command.getSourceId());
		if (active != null && checksum.equals(active.getChecksum())) {
			return ingestion.importDocument(importRequest(command, active));
		}
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("mediaType", command.getMediaType());
		metadata.put("filename", command.getName());
		metadata.put("importOptions", new LinkedHashMap<String, Object>(command.getOptions()));
		if (command.getSourceUri() != null) metadata.put("originUrl", command.getSourceUri());
		String objectReference = null;
		String inlineContent = null;
		if ("text".equals(command.getSourceType())) {
			inlineContent = new String(content, StandardCharsets.UTF_8);
		} else {
			Map<String, Object> storageMetadata = new LinkedHashMap<String, Object>();
			storageMetadata.put(S3_KEY, required(knowledge.getMetadata().get(S3_KEY), S3_KEY));
			try {
				TinyflowObjectStorage.StoredObject stored = objectStorage.put(command.getTenantId(), command.getName(),
						new ByteArrayInputStream(content), command.getMediaType(), storageMetadata);
				objectReference = stored.getObjectKey();
				metadata.putAll(stored.getMetadata());
				metadata.put("size", stored.getSize());
			} catch (IOException exception) {
				throw new IllegalStateException("Knowledge object upload failed", exception);
			}
		}
		TinyflowDocument document = TinyflowDocument.builder().tenantId(command.getTenantId())
				.knowledgeId(command.getKnowledgeId()).sourceId(command.getSourceId())
				.sourceType(command.getSourceType()).documentId(UUID.randomUUID().toString()).checksum(checksum)
				.name(command.getName()).content(inlineContent).sourceUri(objectReference).sourceVersion(checksum)
				.metadata(metadata).enabled(true).build();
		TinyflowDocumentImport request = importRequest(command, document);
		try {
			TinyflowIngestionTask task = ingestion.importDocument(request);
			if (objectReference != null && !document.getDocumentId().equals(task.getDocumentId())) {
				objectStorage.delete(command.getTenantId(), objectReference);
			}
			return task;
		} catch (RuntimeException exception) {
			if (objectReference != null) {
				try { objectStorage.delete(command.getTenantId(), objectReference); }
				catch (RuntimeException cleanupFailure) {
					exception.addSuppressed(cleanupFailure);
					persistTemporaryObjectCleanup(command, document, objectReference, exception);
				}
			}
			throw exception;
		}
	}

	private void persistTemporaryObjectCleanup(ImportCommand command, TinyflowDocument document,
			String objectReference, RuntimeException original) {
		try {
			repository.saveVectorCompensation(new TinyflowKnowledgeRepository.VectorCompensation(
					UUID.randomUUID().toString(), command.getTenantId(), command.getKnowledgeId(), command.getSourceId(),
					document.getDocumentId(), Collections.<String>emptyList(),
					TinyflowVectorCompensator.DELETE_TEMPORARY_OBJECT,
					TinyflowKnowledgeRepository.VectorCompensation.OBJECT_PENDING, objectReference));
		} catch (RuntimeException persistenceFailure) {
			original.addSuppressed(persistenceFailure);
		}
	}

	private TinyflowDocumentImport importRequest(ImportCommand command, TinyflowDocument document) {
		Map<String, Object> options = new LinkedHashMap<String, Object>(command.getOptions());
		options.put("mediaType", command.getMediaType());
		return TinyflowDocumentImport.builder().providerKey(LOCAL).tenantId(command.getTenantId())
				.knowledgeId(command.getKnowledgeId()).document(document).options(options).build();
	}

	@Override public PageResult<TinyflowChunk> listChunks(String tenantId, String knowledgeId,
			String documentId, int page, int size) {
		List<TinyflowChunk> values = repository.findChunksByDocument(tenantId, knowledgeId, documentId);
		Collections.sort(values, new Comparator<TinyflowChunk>() {
			@Override public int compare(TinyflowChunk left, TinyflowChunk right) {
				int leftPosition = left.getPosition() == null ? Integer.MAX_VALUE : left.getPosition();
				int rightPosition = right.getPosition() == null ? Integer.MAX_VALUE : right.getPosition();
				int compared = Integer.compare(leftPosition, rightPosition);
				return compared != 0 ? compared : left.getChunkId().compareTo(right.getChunkId());
			}
		});
		return page(values, page, size);
	}

	@Override public TinyflowChunk updateChunk(String tenantId, String knowledgeId, String chunkId,
			boolean enabled, String content, Map<String, Object> metadata) {
		return maintenance.updateChunk(tenantId, knowledgeId, chunkId, enabled, content, metadata);
	}

	@Override public PageResult<TinyflowIngestionTask> listTasks(String tenantId, String knowledgeId,
			int page, int size) {
		List<TinyflowIngestionTask> values = repository.findTasks(tenantId, knowledgeId);
		Collections.sort(values, Comparator.comparing(TinyflowIngestionTask::getTaskId));
		return page(values, page, size);
	}

	@Override public TinyflowIngestionTask retry(String tenantId, String knowledgeId, String taskId) {
		TinyflowIngestionTask task = repository.findTask(tenantId, knowledgeId, taskId);
		if (task == null || !"FAILED".equals(task.getStatus())) {
			throw new IllegalArgumentException("Only failed ingestion tasks can be retried");
		}
		String sourceId = required(task.getMetadata().get("sourceId"), "sourceId");
		String checksum = required(task.getMetadata().get("checksum"), "checksum");
		TinyflowDocument document = repository.findBySourceAndChecksum(tenantId, knowledgeId, sourceId, checksum);
		if (document == null) throw new IllegalArgumentException("Failed ingestion document was not found");
		Map<String, Object> options = map(document.getMetadata().get("importOptions"));
		Object mediaType = document.getMetadata().get("mediaType");
		if (mediaType != null) options.put("mediaType", String.valueOf(mediaType));
		return ingestion.importDocument(TinyflowDocumentImport.builder().providerKey(LOCAL).tenantId(tenantId)
				.knowledgeId(knowledgeId).document(document).options(options).build());
	}

	@Override public TinyflowIngestionTask cancel(String tenantId, String knowledgeId, String taskId) {
		return ingestion.cancel(tenantId, knowledgeId, taskId);
	}

	@Override public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
		if (request == null || !LOCAL.equals(request.getProviderKey())) {
			throw new IllegalArgumentException("Recall test supports fw-plus-local only");
		}
		return providers.require(LOCAL).retrieve(request);
	}

	private void save(TinyflowKnowledgeDefinition definition, long expectedVersion) {
		String storeKey = required(definition.getMetadata().get(VECTOR_STORE_KEY), VECTOR_STORE_KEY);
		TinyflowVectorStoreDescriptor descriptor = null;
		for (TinyflowVectorStoreDescriptor value : vectorStores.list()) {
			if (value != null && storeKey.equals(value.getKey())) { descriptor = value; break; }
		}
		if (descriptor == null || vectorStores.get(storeKey) == null) {
			throw new IllegalArgumentException("Vector store is not configured: " + storeKey);
		}
		repository.saveKnowledgeWithVectorStoreOwnership(definition, expectedVersion, storeKey,
				descriptor.isMetadataFilterSupported());
	}

	private KnowledgeRecord record(String tenantId, String knowledgeId) {
		return new KnowledgeRecord(requireKnowledge(tenantId, knowledgeId),
				repository.findKnowledgeVersion(tenantId, knowledgeId));
	}

	private TinyflowKnowledgeDefinition requireKnowledge(String tenantId, String knowledgeId) {
		TinyflowKnowledgeDefinition value = repository.findKnowledge(tenantId, knowledgeId);
		if (value == null) throw new IllegalArgumentException("Knowledge was not found");
		return value;
	}

	private void validateDefinition(TinyflowKnowledgeDefinition definition) {
		if (definition == null || !LOCAL.equals(definition.getProviderKey())) {
			throw new IllegalArgumentException("providerKey must be fw-plus-local");
		}
		for (String key : definition.getMetadata().keySet()) {
			if (!KNOWLEDGE_METADATA_KEYS.contains(key)) {
				throw new IllegalArgumentException("Unsupported knowledge metadata: " + key);
			}
		}
		String embeddingModelKey = required(definition.getMetadata().get(EMBEDDING_MODEL_KEY), EMBEDDING_MODEL_KEY);
		String vectorStoreKey = required(definition.getMetadata().get(VECTOR_STORE_KEY), VECTOR_STORE_KEY);
		String s3Key = required(definition.getMetadata().get(S3_KEY), S3_KEY);
		requireOption(optionProvider.hasEmbeddingModel(embeddingModelKey), embeddingModelKey, EMBEDDING_MODEL_KEY);
		requireOption(optionProvider.hasVectorStore(vectorStoreKey), vectorStoreKey, VECTOR_STORE_KEY);
		requireOption(optionProvider.hasS3Storage(s3Key), s3Key, S3_KEY);
	}

	private void requireOption(boolean configured, String key, String field) {
		if (!configured) throw new IllegalArgumentException(field + " is not configured: " + key);
	}

	private <T> PageResult<T> page(List<T> values, int page, int size) {
		if (page < 1 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page request");
		long offset = (long) (page - 1) * size;
		if (offset >= values.size()) return new PageResult<T>(page, size, values.size(), Collections.<T>emptyList());
		int from = (int) offset;
		int to = Math.min(values.size(), from + size);
		return new PageResult<T>(page, size, values.size(), new ArrayList<T>(values.subList(from, to)));
	}

	private Map<String, Object> map(Object value) {
		if (!(value instanceof Map)) return new LinkedHashMap<String, Object>();
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
			if (entry.getKey() != null) result.put(String.valueOf(entry.getKey()), entry.getValue());
		}
		return result;
	}

	private String required(Object value, String field) {
		String text = value == null ? null : String.valueOf(value).trim();
		if (text == null || text.length() == 0) throw new IllegalArgumentException(field + " must not be blank");
		return text;
	}

	private String sha256(byte[] content) {
		try {
			byte[] values = MessageDigest.getInstance("SHA-256").digest(content);
			StringBuilder result = new StringBuilder(values.length * 2);
			for (byte value : values) result.append(String.format("%02x", value & 0xff));
			return result.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	interface IngestionGateway {
		TinyflowIngestionTask importDocument(TinyflowDocumentImport request);
		TinyflowIngestionTask cancel(String tenantId, String knowledgeId, String taskId);
	}
}
