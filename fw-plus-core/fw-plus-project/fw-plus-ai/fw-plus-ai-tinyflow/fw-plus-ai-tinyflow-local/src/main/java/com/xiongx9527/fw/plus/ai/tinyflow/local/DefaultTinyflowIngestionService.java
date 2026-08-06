package com.xiongx9527.fw.plus.ai.tinyflow.local;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentImport;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class DefaultTinyflowIngestionService {

	public static final String PROVIDER_KEY = "fw-plus-local";

	private final TinyflowKnowledgeRepository repository;
	private final TinyflowObjectStorage objectStorage;
	private final TinyflowDocumentParser parser;
	private final Map<String, TinyflowChunkStrategy> chunkStrategies;
	private final IngestionIndex ingestionIndex;
	private final Executor executor;
	private final Map<String, TaskControl> controls = new ConcurrentHashMap<String, TaskControl>();
	private final Map<String, TaskControl> inFlight = new ConcurrentHashMap<String, TaskControl>();

	public DefaultTinyflowIngestionService(TinyflowKnowledgeRepository repository,
			TinyflowObjectStorage objectStorage, TinyflowDocumentParser parser,
			Map<String, TinyflowChunkStrategy> chunkStrategies, IngestionIndex ingestionIndex,
			Executor executor) {
		if (repository == null || objectStorage == null || parser == null || ingestionIndex == null
				|| executor == null || chunkStrategies == null || chunkStrategies.isEmpty()) {
			throw new IllegalArgumentException("Ingestion dependencies must not be null or empty");
		}
		this.repository = repository;
		this.objectStorage = objectStorage;
		this.parser = parser;
		this.chunkStrategies = Collections.unmodifiableMap(
				new LinkedHashMap<String, TinyflowChunkStrategy>(chunkStrategies));
		this.ingestionIndex = ingestionIndex;
		this.executor = executor;
	}

	public TinyflowIngestionTask importDocument(final TinyflowDocumentImport request) {
		validate(request);
		TinyflowDocument input = request.getDocument();
		String checksum = input.getChecksum();
		String sourceKey = sourceKey(request, input.getSourceId());
		final TaskControl control;
		TinyflowIngestionTask pending;
		synchronized (inFlight) {
			TaskControl existing = inFlight.get(sourceKey);
			if (existing != null && !isTerminal(existing.status)) {
				if (checksum.equals(existing.document.getChecksum())) {
					return snapshot(existing, existing.status, null, null);
				}
				throw new IllegalStateException("An import is already in progress for this source");
			}
			TinyflowDocument active = repository.findActiveDocument(request.getTenantId(),
					request.getKnowledgeId(), input.getSourceId());
			if (active != null && checksum.equals(active.getChecksum())) {
				Map<String, Object> taskMetadata = metadata("COMPLETED", 0, active.getChunks().size(), 0);
				taskMetadata.put("sourceId", active.getSourceId());
				taskMetadata.put("checksum", active.getChecksum());
				TinyflowIngestionTask completed = task(UUID.randomUUID().toString(), request, active.getDocumentId(),
						"COMPLETED", "Document already imported", taskMetadata);
				repository.saveTask(completed);
				return completed;
			}
			TinyflowDocument document = createDocumentVersion(input, request, checksum);
			repository.saveDocument(document);
			control = new TaskControl(UUID.randomUUID().toString(), request, document,
					retryCount(request, input.getSourceId(), checksum), sourceKey);
			controls.put(control.taskId, control);
			inFlight.put(sourceKey, control);
			try {
				pending = transition(control, "PENDING", null, null);
			} catch (RuntimeException exception) {
				controls.remove(control.taskId, control);
				inFlight.remove(sourceKey, control);
				throw exception;
			}
		}
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					process(control);
				}
			});
		} catch (RuntimeException exception) {
			try {
				fail(control, "PENDING");
			} finally {
				inFlight.remove(sourceKey, control);
			}
		}
		return pending;
	}

	public TinyflowIngestionTask cancel(String tenantId, String knowledgeId, String taskId) {
		TaskControl control = controls.get(taskId);
		if (control == null || !control.request.getTenantId().equals(tenantId)
				|| !control.request.getKnowledgeId().equals(knowledgeId)) {
			throw new IllegalArgumentException("Ingestion task not found");
		}
		synchronized (control) {
			if (isTerminal(control.status)) {
				return snapshotLocked(control, control.status, null, null);
			}
			TinyflowIngestionTask task = transitionLocked(control, "CANCELLING", null, null);
			control.cancelled = true;
			return task;
		}
	}

	private void process(TaskControl control) {
		try {
			if (!enterStage(control, "PARSING")) {
				finishCancelled(control);
				return;
			}
			TinyflowParsedDocument parsed;
			try (InputStream input = openInput(control.document)) {
				TinyflowDocumentParser.ParseContext context = new TinyflowDocumentParser.ParseContext(
						control.request.getTenantId(), control.request.getKnowledgeId(),
						control.document.getSourceId(), control.document.getSourceType(),
						control.document.getDocumentId(), mediaType(control),
						control.document.getName(), control.request.getOptions());
				parsed = parser.parse(input, context);
			}
			synchronized (control) {
				control.parsedSectionCount = parsed.getSections().size();
			}
			if (cancelRequested(control)) {
				finishCancelled(control);
				return;
			}

			if (!enterStage(control, "CHUNKING")) {
				finishCancelled(control);
				return;
			}
			TinyflowChunkStrategy strategy = chunkStrategies.get(strategyKey(control.request));
			if (strategy == null) {
				throw new IllegalArgumentException("Unsupported chunk strategy");
			}
			Map<String, Object> chunkOptions = new LinkedHashMap<String, Object>(control.request.getOptions());
			chunkOptions.put("sourceUri", control.document.getSourceUri());
			chunkOptions.put("sourceVersion", control.document.getSourceVersion());
			List<TinyflowChunk> chunks = strategy.chunk(parsed, new TinyflowChunkStrategy.ChunkOptions(
					control.document.getTenantId(), control.document.getKnowledgeId(),
					control.document.getSourceId(), control.document.getSourceType(),
					control.document.getDocumentId(), chunkOptions));
			synchronized (control) {
				control.chunkCount = chunks.size();
			}
			if (cancelRequested(control)) {
				finishCancelled(control);
				return;
			}

			if (!enterStage(control, "EMBEDDING")) {
				finishCancelled(control);
				return;
			}
			ingestionIndex.embed(control.document.getTenantId(), control.document.getKnowledgeId(),
					control.document.getDocumentId(), chunks);
			if (cancelRequested(control)) {
				finishCancelled(control);
				return;
			}

			if (!enterStage(control, "INDEXING")) {
				finishCancelled(control);
				return;
			}
			Map<String, List<String>> indexed;
			try {
				indexed = ingestionIndex.index(control.document.getTenantId(),
						control.document.getKnowledgeId(), control.document.getDocumentId(), chunks);
			} catch (IngestionIndex.PartialIndexException exception) {
				recordIndexedVectors(control, chunks, exception.getPartialResult());
				throw exception;
			}
			recordIndexedVectors(control, chunks, indexed);
			validateIndexKeys(chunks, indexed);
			List<TinyflowChunk> indexedChunks = withVectorIds(chunks, indexed);
			if (cancelRequested(control)) {
				compensate(control);
				finishCancelled(control);
				return;
			}

			Map<String, Object> documentMetadata = new LinkedHashMap<String, Object>(control.document.getMetadata());
			documentMetadata.putAll(parsed.getMetadata());
			TinyflowDocument completedDocument = TinyflowDocument.builder()
					.tenantId(control.document.getTenantId()).knowledgeId(control.document.getKnowledgeId())
					.sourceId(control.document.getSourceId()).sourceType(control.document.getSourceType())
					.documentId(control.document.getDocumentId()).checksum(control.document.getChecksum())
					.name(control.document.getName())
					.content(parsed.getContent()).sourceUri(control.document.getSourceUri())
					.sourceVersion(control.document.getSourceVersion()).metadata(documentMetadata)
					.chunks(indexedChunks).enabled(control.document.isEnabled()).build();
			if (!complete(control, completedDocument, indexedChunks)) {
				compensate(control);
				finishCancelled(control);
			}
		} catch (Exception exception) {
			compensate(control);
			fail(control, control.stage);
		} finally {
			inFlight.remove(control.sourceKey, control);
		}
	}

	private boolean enterStage(TaskControl control, String stage) {
		synchronized (control) {
			if (control.cancelled || isTerminal(control.status)) {
				return false;
			}
			transitionLocked(control, stage, null, null);
			return true;
		}
	}

	private void fail(TaskControl control, String failedStage) {
		synchronized (control) {
			if (control.cancelled) {
				finishCancelledLocked(control);
				return;
			}
			if (!isTerminal(control.status)) {
				transitionLocked(control, "FAILED", "Ingestion failed during " + safeStage(failedStage),
						safeStage(failedStage));
			}
		}
	}

	private void finishCancelled(TaskControl control) {
		synchronized (control) {
			finishCancelledLocked(control);
		}
	}

	private void finishCancelledLocked(TaskControl control) {
		if (!isTerminal(control.status)) {
			transitionLocked(control, "CANCELLED", null, null);
		}
	}

	private boolean cancelRequested(TaskControl control) {
		synchronized (control) {
			return control.cancelled;
		}
	}

	private TinyflowIngestionTask transition(TaskControl control, String status,
			String message, String failedStage) {
		synchronized (control) {
			return transitionLocked(control, status, message, failedStage);
		}
	}

	private TinyflowIngestionTask transitionLocked(TaskControl control, String status,
			String message, String failedStage) {
		if (!allowsTransition(control.status, status)) {
			throw new IllegalStateException("Illegal ingestion status transition");
		}
		TinyflowIngestionTask task = snapshotLocked(control, status, message, failedStage);
		repository.saveTask(task);
		control.status = status;
		control.stage = status;
		return task;
	}

	private TinyflowIngestionTask snapshot(TaskControl control, String status, String message, String failedStage) {
		synchronized (control) {
			return snapshotLocked(control, status, message, failedStage);
		}
	}

	private TinyflowIngestionTask snapshotLocked(TaskControl control, String status,
			String message, String failedStage) {
		Map<String, Object> taskMetadata = metadata(status, control.parsedSectionCount,
				control.chunkCount, control.retryCount, failedStage);
		taskMetadata.put("sourceId", control.document.getSourceId());
		taskMetadata.put("checksum", control.document.getChecksum());
		return task(control.taskId, control.request, control.document.getDocumentId(), status, message, taskMetadata);
	}

	private TinyflowIngestionTask task(String taskId, TinyflowDocumentImport request, String documentId,
			String status, String message, Map<String, Object> metadata) {
		return TinyflowIngestionTask.builder().taskId(taskId).providerKey(PROVIDER_KEY)
				.tenantId(request.getTenantId()).knowledgeId(request.getKnowledgeId())
				.documentId(documentId).status(status).message(message).metadata(metadata).build();
	}

	private Map<String, Object> metadata(String stage, int parsedSectionCount, int chunkCount, int retryCount) {
		return metadata(stage, parsedSectionCount, chunkCount, retryCount, null);
	}

	private Map<String, Object> metadata(String stage, int parsedSectionCount, int chunkCount,
			int retryCount, String failedStage) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("stage", stage);
		metadata.put("parsedSectionCount", parsedSectionCount);
		metadata.put("chunkCount", chunkCount);
		metadata.put("retryCount", retryCount);
		if (failedStage != null) {
			metadata.put("failedStage", failedStage);
		}
		return metadata;
	}

	private boolean complete(TaskControl control, TinyflowDocument completedDocument,
			List<TinyflowChunk> indexedChunks) {
		synchronized (control) {
			if (control.cancelled) {
				return false;
			}
			if (!allowsTransition(control.status, "COMPLETED")) {
				throw new IllegalStateException("Illegal ingestion status transition");
			}
			TinyflowIngestionTask completedTask = snapshotLocked(control, "COMPLETED", null, null);
			repository.completeIngestion(completedDocument, indexedChunks, completedTask);
			control.document = completedDocument;
			control.status = "COMPLETED";
			control.stage = "COMPLETED";
			return true;
		}
	}

	private void compensate(TaskControl control) {
		final List<String> vectorIds;
		synchronized (control) {
			if (!control.indexed || control.compensationAttempted || control.indexedVectorIds.isEmpty()) {
				return;
			}
			control.compensationAttempted = true;
			vectorIds = new ArrayList<String>(control.indexedVectorIds);
		}
		TinyflowKnowledgeRepository.VectorCompensation compensation =
				new TinyflowKnowledgeRepository.VectorCompensation(UUID.randomUUID().toString(),
						control.document.getTenantId(), control.document.getKnowledgeId(),
						control.document.getSourceId(), control.document.getDocumentId(), vectorIds, "delete");
		try {
			repository.saveVectorCompensation(compensation);
		} catch (RuntimeException exception) {
			return;
		}
		try {
			ingestionIndex.delete(control.document.getTenantId(), control.document.getKnowledgeId(), vectorIds);
			repository.deleteVectorCompensation(control.document.getTenantId(), control.document.getKnowledgeId(),
					compensation.getCompensationId());
		} catch (Exception ignored) {
			// The durable compensation record is intentionally retained for a later retry.
		}
	}

	private List<TinyflowChunk> withVectorIds(List<TinyflowChunk> chunks,
			Map<String, List<String>> indexed) throws IOException {
		if (indexed == null) {
			throw new IOException("Index result is unavailable");
		}
		List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
		for (TinyflowChunk chunk : chunks) {
			List<String> vectorIds = indexed.get(chunk.getChunkId());
			if (vectorIds == null || vectorIds.isEmpty()) {
				throw new IOException("Index result is incomplete");
			}
			result.add(TinyflowChunk.builder().tenantId(chunk.getTenantId()).knowledgeId(chunk.getKnowledgeId())
					.sourceId(chunk.getSourceId()).sourceType(chunk.getSourceType())
					.documentId(chunk.getDocumentId()).chunkId(chunk.getChunkId()).content(chunk.getContent())
					.sourceUri(chunk.getSourceUri()).sourceVersion(chunk.getSourceVersion())
					.location(chunk.getLocation()).position(chunk.getPosition()).parentChunkId(chunk.getParentChunkId())
					.tokenCount(chunk.getTokenCount()).metadata(chunk.getMetadata()).enabled(chunk.isEnabled())
					.vectorIds(vectorIds).build());
		}
		return result;
	}

	private void recordIndexedVectors(TaskControl control, List<TinyflowChunk> chunks,
			Map<String, List<String>> indexed) {
		Set<String> expected = chunkIds(chunks);
		Map<String, List<String>> currentBatch = new LinkedHashMap<String, List<String>>();
		if (indexed != null) {
			for (Map.Entry<String, List<String>> entry : indexed.entrySet()) {
				if (expected.contains(entry.getKey())) {
					currentBatch.put(entry.getKey(), entry.getValue());
				}
			}
		}
		List<String> vectorIds = collectVectorIds(currentBatch);
		synchronized (control) {
			control.indexedVectorIds = vectorIds;
			control.indexed = !vectorIds.isEmpty();
		}
	}

	private void validateIndexKeys(List<TinyflowChunk> chunks,
			Map<String, List<String>> indexed) throws IOException {
		if (indexed == null || !chunkIds(chunks).equals(indexed.keySet())) {
			throw new IOException("Index result keys do not match current batch");
		}
	}

	private Set<String> chunkIds(List<TinyflowChunk> chunks) {
		Set<String> values = new LinkedHashSet<String>();
		for (TinyflowChunk chunk : chunks) {
			values.add(chunk.getChunkId());
		}
		return values;
	}

	private List<String> collectVectorIds(List<TinyflowChunk> chunks) {
		Set<String> values = new LinkedHashSet<String>();
		for (TinyflowChunk chunk : chunks) {
			values.addAll(chunk.getVectorIds());
		}
		return new ArrayList<String>(values);
	}

	private List<String> collectVectorIds(Map<String, List<String>> indexed) {
		if (indexed == null) {
			return Collections.emptyList();
		}
		Set<String> values = new LinkedHashSet<String>();
		for (List<String> vectorIds : indexed.values()) {
			if (vectorIds != null) {
				values.addAll(vectorIds);
			}
		}
		return new ArrayList<String>(values);
	}

	private int retryCount(TinyflowDocumentImport request, String sourceId, String checksum) {
		int count = 0;
		for (TinyflowIngestionTask task : repository.findTasks(request.getTenantId(), request.getKnowledgeId())) {
			if ("FAILED".equals(task.getStatus()) && sourceId.equals(task.getMetadata().get("sourceId"))
					&& checksum.equals(task.getMetadata().get("checksum"))) {
				count++;
			}
		}
		return count;
	}

	private TinyflowDocument createDocumentVersion(TinyflowDocument input,
			TinyflowDocumentImport request, String checksum) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>(input.getMetadata());
		metadata.put("checksum", checksum);
		return TinyflowDocument.builder().tenantId(request.getTenantId()).knowledgeId(request.getKnowledgeId())
				.sourceId(input.getSourceId()).sourceType(input.getSourceType())
				.documentId(UUID.randomUUID().toString()).checksum(checksum)
				.name(input.getName()).content(input.getContent())
				.sourceUri(input.getSourceUri())
				.sourceVersion(input.getSourceVersion() == null ? checksum : input.getSourceVersion())
				.metadata(metadata).enabled(input.isEnabled()).build();
	}

	private InputStream openInput(TinyflowDocument document) throws IOException {
		if (document.getContent() != null) {
			return new ByteArrayInputStream(document.getContent().getBytes(StandardCharsets.UTF_8));
		}
		if (document.getSourceUri() == null || document.getSourceUri().trim().isEmpty()) {
			throw new IOException("Document input is unavailable");
		}
		return objectStorage.get(document.getTenantId(), document.getSourceUri());
	}

	private String mediaType(TaskControl control) {
		Object option = control.request.getOptions().get("mediaType");
		Object metadata = control.document.getMetadata().get("mediaType");
		return option != null ? String.valueOf(option) : metadata == null ? null : String.valueOf(metadata);
	}

	private String strategyKey(TinyflowDocumentImport request) {
		Object value = request.getOptions().get("chunkStrategy");
		return value == null ? "normal" : String.valueOf(value);
	}

	private void validate(TinyflowDocumentImport request) {
		if (request == null) {
			throw new IllegalArgumentException("request must not be null");
		}
		if (!PROVIDER_KEY.equals(request.getProviderKey())) {
			throw new IllegalArgumentException("providerKey must be fw-plus-local");
		}
		TinyflowDocument document = request.getDocument();
		if (!request.getTenantId().equals(document.getTenantId())
				|| !request.getKnowledgeId().equals(document.getKnowledgeId())) {
			throw new IllegalArgumentException("Document identity does not match import identity");
		}
	}

	private String sourceKey(TinyflowDocumentImport request, String sourceId) {
		return request.getTenantId() + "|" + request.getKnowledgeId() + "|" + sourceId;
	}

	private String safeStage(String stage) {
		return stage == null || !stage.matches("[A-Z]+") ? "UNKNOWN" : stage;
	}

	private boolean isTerminal(String status) {
		return "COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
	}

	private boolean allowsTransition(String current, String next) {
		if (current.equals(next)) {
			return "PENDING".equals(current);
		}
		if ("PENDING".equals(current)) {
			return "PARSING".equals(next) || "CANCELLING".equals(next) || "FAILED".equals(next);
		}
		if ("PARSING".equals(current)) {
			return "CHUNKING".equals(next) || "CANCELLING".equals(next) || "FAILED".equals(next);
		}
		if ("CHUNKING".equals(current)) {
			return "EMBEDDING".equals(next) || "CANCELLING".equals(next) || "FAILED".equals(next);
		}
		if ("EMBEDDING".equals(current)) {
			return "INDEXING".equals(next) || "CANCELLING".equals(next) || "FAILED".equals(next);
		}
		if ("INDEXING".equals(current)) {
			return "COMPLETED".equals(next) || "CANCELLING".equals(next) || "FAILED".equals(next);
		}
		return "CANCELLING".equals(current) && "CANCELLED".equals(next);
	}

	public interface IngestionIndex {
		void embed(String tenantId, String knowledgeId, String documentId,
				List<TinyflowChunk> chunks) throws Exception;

		Map<String, List<String>> index(String tenantId, String knowledgeId, String documentId,
				List<TinyflowChunk> chunks) throws Exception;

		void delete(String tenantId, String knowledgeId, List<String> vectorIds) throws Exception;

		final class PartialIndexException extends Exception {
			private static final long serialVersionUID = 1L;
			private final Map<String, List<String>> partialResult;

			public PartialIndexException(String message, Map<String, List<String>> partialResult) {
				super(message);
				Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
				if (partialResult != null) {
					for (Map.Entry<String, List<String>> entry : partialResult.entrySet()) {
						List<String> vectorIds = entry.getValue() == null ? Collections.<String>emptyList()
								: Collections.unmodifiableList(new ArrayList<String>(entry.getValue()));
						copy.put(entry.getKey(), vectorIds);
					}
				}
				this.partialResult = Collections.unmodifiableMap(copy);
			}

			public Map<String, List<String>> getPartialResult() { return partialResult; }
		}
	}

	private static final class TaskControl {
		private final String taskId;
		private final TinyflowDocumentImport request;
		private final String sourceKey;
		private TinyflowDocument document;
		private String stage = "PENDING";
		private String status = "PENDING";
		private boolean cancelled;
		private boolean indexed;
		private boolean compensationAttempted;
		private List<String> indexedVectorIds = Collections.emptyList();
		private int parsedSectionCount;
		private int chunkCount;
		private final int retryCount;

		private TaskControl(String taskId, TinyflowDocumentImport request, TinyflowDocument document,
				int retryCount, String sourceKey) {
			this.taskId = taskId;
			this.request = request;
			this.document = document;
			this.retryCount = retryCount;
			this.sourceKey = sourceKey;
		}
	}
}
