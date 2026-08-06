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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultTinyflowIngestionService {

	public static final String PROVIDER_KEY = "fw-plus-local";

	private final TinyflowKnowledgeRepository repository;
	private final TinyflowObjectStorage objectStorage;
	private final TinyflowDocumentParser parser;
	private final Map<String, TinyflowChunkStrategy> chunkStrategies;
	private final IngestionIndex ingestionIndex;
	private final Executor executor;
	private final Map<String, TaskControl> controls = new ConcurrentHashMap<String, TaskControl>();

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
		String checksum = requiredMetadata(input, "checksum");
		TinyflowDocument same = repository.findBySourceAndChecksum(request.getTenantId(),
				request.getKnowledgeId(), input.getSourceId(), checksum);
		TinyflowDocument active = repository.findActiveDocument(request.getTenantId(),
				request.getKnowledgeId(), input.getSourceId());
		if (same != null && active != null && same.getDocumentId().equals(active.getDocumentId())) {
			TinyflowIngestionTask completed = task(UUID.randomUUID().toString(), request, same.getDocumentId(),
					"COMPLETED", "Document already imported", metadata("COMPLETED", 0, same.getChunks().size(), 0));
			repository.saveTask(completed);
			return completed;
		}

		TinyflowDocument document = createDocumentVersion(input, request, checksum);
		repository.saveDocument(document);
		final TaskControl control = new TaskControl(UUID.randomUUID().toString(), request, document);
		controls.put(control.taskId, control);
		TinyflowIngestionTask pending = save(control, "PENDING", null, null);
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					process(control);
				}
			});
		} catch (RuntimeException exception) {
			fail(control, "PENDING");
		}
		return pending;
	}

	public TinyflowIngestionTask cancel(String tenantId, String knowledgeId, String taskId) {
		TaskControl control = controls.get(taskId);
		if (control == null || !control.request.getTenantId().equals(tenantId)
				|| !control.request.getKnowledgeId().equals(knowledgeId)) {
			throw new IllegalArgumentException("Ingestion task not found");
		}
		if (isTerminal(control.status)) {
			return snapshot(control, control.status, null, null);
		}
		control.cancelled.set(true);
		return save(control, "CANCELLING", null, null);
	}

	private void process(TaskControl control) {
		if (cancelled(control)) {
			return;
		}
		try {
			control.stage = "PARSING";
			save(control, "PARSING", null, null);
			TinyflowParsedDocument parsed;
			try (InputStream input = openInput(control.document)) {
				TinyflowDocumentParser.ParseContext context = new TinyflowDocumentParser.ParseContext(
						control.request.getTenantId(), control.request.getKnowledgeId(),
						control.document.getSourceId(), control.document.getSourceType(),
						control.document.getDocumentId(), mediaType(control),
						control.document.getName(), control.request.getOptions());
				parsed = parser.parse(input, context);
			}
			control.parsedSectionCount = parsed.getSections().size();
			if (cancelled(control)) {
				return;
			}

			control.stage = "CHUNKING";
			save(control, "CHUNKING", null, null);
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
			control.chunkCount = chunks.size();
			if (cancelled(control)) {
				return;
			}

			control.stage = "EMBEDDING";
			save(control, "EMBEDDING", null, null);
			ingestionIndex.embed(control.document.getTenantId(), control.document.getKnowledgeId(),
					control.document.getDocumentId(), chunks);
			if (cancelled(control)) {
				return;
			}

			control.stage = "INDEXING";
			save(control, "INDEXING", null, null);
			ingestionIndex.index(control.document.getTenantId(), control.document.getKnowledgeId(),
					control.document.getDocumentId(), chunks);
			if (cancelled(control)) {
				return;
			}

			Map<String, Object> documentMetadata = new LinkedHashMap<String, Object>(control.document.getMetadata());
			documentMetadata.putAll(parsed.getMetadata());
			TinyflowDocument completedDocument = TinyflowDocument.builder()
					.tenantId(control.document.getTenantId()).knowledgeId(control.document.getKnowledgeId())
					.sourceId(control.document.getSourceId()).sourceType(control.document.getSourceType())
					.documentId(control.document.getDocumentId()).name(control.document.getName())
					.content(parsed.getContent()).sourceUri(control.document.getSourceUri())
					.sourceVersion(control.document.getSourceVersion()).metadata(documentMetadata)
					.chunks(chunks).build();
			repository.saveChunks(chunks);
			repository.saveDocument(completedDocument);
			repository.activateDocumentVersion(completedDocument.getTenantId(), completedDocument.getKnowledgeId(),
					completedDocument.getSourceId(), completedDocument.getDocumentId());
			control.stage = "COMPLETED";
			control.document = completedDocument;
			save(control, "COMPLETED", null, null);
		} catch (Exception exception) {
			fail(control, control.stage);
		}
	}

	private boolean cancelled(TaskControl control) {
		if (!control.cancelled.get()) {
			return false;
		}
		control.stage = "CANCELLED";
		save(control, "CANCELLED", null, null);
		return true;
	}

	private void fail(TaskControl control, String failedStage) {
		control.stage = "FAILED";
		save(control, "FAILED", "Ingestion failed during " + safeStage(failedStage), safeStage(failedStage));
	}

	private TinyflowIngestionTask save(TaskControl control, String status, String message, String failedStage) {
		TinyflowIngestionTask task = snapshot(control, status, message, failedStage);
		control.status = status;
		repository.saveTask(task);
		return task;
	}

	private TinyflowIngestionTask snapshot(TaskControl control, String status, String message, String failedStage) {
		return task(control.taskId, control.request, control.document.getDocumentId(), status, message,
				metadata(status, control.parsedSectionCount, control.chunkCount, control.retryCount, failedStage));
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

	private TinyflowDocument createDocumentVersion(TinyflowDocument input,
			TinyflowDocumentImport request, String checksum) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>(input.getMetadata());
		metadata.put("checksum", checksum);
		return TinyflowDocument.builder().tenantId(request.getTenantId()).knowledgeId(request.getKnowledgeId())
				.sourceId(input.getSourceId()).sourceType(input.getSourceType())
				.documentId(UUID.randomUUID().toString()).name(input.getName()).content(input.getContent())
				.sourceUri(input.getSourceUri())
				.sourceVersion(input.getSourceVersion() == null ? checksum : input.getSourceVersion())
				.metadata(metadata).build();
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
		requiredMetadata(document, "checksum");
	}

	private String requiredMetadata(TinyflowDocument document, String key) {
		Object value = document.getMetadata().get(key);
		if (value == null || String.valueOf(value).trim().isEmpty()) {
			throw new IllegalArgumentException(key + " must not be blank");
		}
		return String.valueOf(value);
	}

	private String safeStage(String stage) {
		return stage == null || !stage.matches("[A-Z]+") ? "UNKNOWN" : stage;
	}

	private boolean isTerminal(String status) {
		return "COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
	}

	public interface IngestionIndex {
		void embed(String tenantId, String knowledgeId, String documentId,
				List<TinyflowChunk> chunks) throws Exception;

		void index(String tenantId, String knowledgeId, String documentId,
				List<TinyflowChunk> chunks) throws Exception;
	}

	private static final class TaskControl {
		private final String taskId;
		private final TinyflowDocumentImport request;
		private final AtomicBoolean cancelled = new AtomicBoolean();
		private TinyflowDocument document;
		private String stage = "PENDING";
		private String status = "PENDING";
		private int parsedSectionCount;
		private int chunkCount;
		private int retryCount;

		private TaskControl(String taskId, TinyflowDocumentImport request, TinyflowDocument document) {
			this.taskId = taskId;
			this.request = request;
			this.document = document;
		}
	}
}
