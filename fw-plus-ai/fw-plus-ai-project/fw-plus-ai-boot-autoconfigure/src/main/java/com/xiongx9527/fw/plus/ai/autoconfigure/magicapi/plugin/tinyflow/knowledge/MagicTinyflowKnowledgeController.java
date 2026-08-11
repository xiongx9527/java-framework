package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify.TinyflowKnowledgeAccessKeyService;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDifyConnectionRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MagicTinyflowKnowledgeController {

	private static final int MAX_PAGE_SIZE = 100;
	private static final int MAX_OPTION_LIMIT = 200;
	private static final int MAX_SECURITY_LIST_SIZE = 200;
	private static final int MAX_IMPORT_BYTES = 20 * 1024 * 1024;
	private static final int MAX_REQUEST_STRING_LENGTH = 8192;
	private static final int MAX_NAME_LENGTH = 512;
	private static final int MAX_URL_LENGTH = 4096;
	private static final int MAX_QUERY_LENGTH = 8192;
	private static final int MAX_STRUCTURED_STRING_LENGTH = 4096;
	private static final int MAX_STRUCTURED_ENTRIES = 64;
	private static final int MAX_STRUCTURED_DEPTH = 8;
	private final MagicApiTinyflowKnowledgeRepository repository;
	private final TinyflowKnowledgeAccessKeyService accessKeyService;
	private final MagicTinyflowTenantResolver tenantResolver;
	private final MagicTinyflowKnowledgePermissionEvaluator permissionEvaluator;
	private final MagicTinyflowKnowledgeAuditSink auditSink;
	private final DifyConnectionTester difyConnectionTester;
	private final MagicTinyflowKnowledgeOperations operations;
	private final MagicTinyflowSafeUrlImporter urlImporter;
	private final MagicTinyflowKnowledgeOptionProvider optionProvider;

	public MagicTinyflowKnowledgeController(MagicApiTinyflowKnowledgeRepository repository,
			TinyflowKnowledgeAccessKeyService accessKeyService, MagicTinyflowTenantResolver tenantResolver,
			MagicTinyflowKnowledgePermissionEvaluator permissionEvaluator, MagicTinyflowKnowledgeAuditSink auditSink) {
		this(repository, accessKeyService, tenantResolver, permissionEvaluator, auditSink, null);
	}

	public MagicTinyflowKnowledgeController(MagicApiTinyflowKnowledgeRepository repository,
			TinyflowKnowledgeAccessKeyService accessKeyService, MagicTinyflowTenantResolver tenantResolver,
			MagicTinyflowKnowledgePermissionEvaluator permissionEvaluator, MagicTinyflowKnowledgeAuditSink auditSink,
			DifyConnectionTester difyConnectionTester) {
		this(repository, accessKeyService, tenantResolver, permissionEvaluator, auditSink,
				difyConnectionTester, null, null, null);
	}

	public MagicTinyflowKnowledgeController(MagicApiTinyflowKnowledgeRepository repository,
			TinyflowKnowledgeAccessKeyService accessKeyService, MagicTinyflowTenantResolver tenantResolver,
			MagicTinyflowKnowledgePermissionEvaluator permissionEvaluator, MagicTinyflowKnowledgeAuditSink auditSink,
			DifyConnectionTester difyConnectionTester, MagicTinyflowKnowledgeOperations operations,
			MagicTinyflowSafeUrlImporter urlImporter, MagicTinyflowKnowledgeOptionProvider optionProvider) {
		if (repository == null || accessKeyService == null || tenantResolver == null
				|| permissionEvaluator == null || auditSink == null) {
			throw new IllegalArgumentException("Knowledge controller dependencies must not be null");
		}
		this.repository = repository;
		this.accessKeyService = accessKeyService;
		this.tenantResolver = tenantResolver;
		this.permissionEvaluator = permissionEvaluator;
		this.auditSink = auditSink;
		this.difyConnectionTester = difyConnectionTester;
		this.operations = operations;
		this.urlImporter = urlImporter;
		this.optionProvider = optionProvider;
	}

	public KnowledgeView createKnowledge(KnowledgeRequest request, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		String knowledgeId = request == null ? null : request.getKnowledgeId();
		try {
			permissionEvaluator.check(tenantId, "knowledge:create", knowledgeId, servletRequest);
			validateKnowledge(request, false);
			MagicTinyflowKnowledgeOperations.KnowledgeRecord result = management().create(definition(tenantId,
					request.getKnowledgeId(), request));
			audit(tenantId, "knowledge:create", knowledgeId, true, null);
			return new KnowledgeView(result);
		} catch (RuntimeException exception) {
			audit(tenantId, "knowledge:create", knowledgeId, false, exception);
			throw exception;
		}
	}

	public KnowledgeView getKnowledge(String knowledgeId, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "knowledge:view", knowledgeId, servletRequest);
			required(knowledgeId, "knowledgeId");
			MagicTinyflowKnowledgeOperations.KnowledgeRecord result = management().find(tenantId, knowledgeId);
			audit(tenantId, "knowledge:view", knowledgeId, true, null);
			return result == null ? null : new KnowledgeView(result);
		} catch (RuntimeException exception) {
			audit(tenantId, "knowledge:view", knowledgeId, false, exception);
			throw exception;
		}
	}

	public PageResponse<KnowledgeView> listKnowledge(int page, int size, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "knowledge:list", null, servletRequest);
			page(page, size);
			MagicTinyflowKnowledgeOperations.PageResult<MagicTinyflowKnowledgeOperations.KnowledgeRecord> source =
					management().listKnowledge(tenantId, page, size);
			List<KnowledgeView> values = new ArrayList<KnowledgeView>();
			for (MagicTinyflowKnowledgeOperations.KnowledgeRecord value : source.getItems()) {
				values.add(new KnowledgeView(value));
			}
			audit(tenantId, "knowledge:list", null, true, null);
			return new PageResponse<KnowledgeView>(source.getPage(), source.getSize(), source.getTotal(), values);
		} catch (RuntimeException exception) {
			audit(tenantId, "knowledge:list", null, false, exception);
			throw exception;
		}
	}

	public KnowledgeView updateKnowledge(String knowledgeId, KnowledgeRequest request,
			MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "knowledge:update", knowledgeId, servletRequest);
			required(knowledgeId, "knowledgeId");
			validateKnowledge(request, true);
			if (!knowledgeId.equals(request.getKnowledgeId())) {
				throw new IllegalArgumentException("knowledgeId does not match request path");
			}
			MagicTinyflowKnowledgeOperations.KnowledgeRecord result = management().update(
					definition(tenantId, knowledgeId, request), request.getExpectedVersion());
			audit(tenantId, "knowledge:update", knowledgeId, true, null);
			return new KnowledgeView(result);
		} catch (RuntimeException exception) {
			audit(tenantId, "knowledge:update", knowledgeId, false, exception);
			throw exception;
		}
	}

	public void deleteKnowledge(String knowledgeId, long expectedVersion,
			MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "knowledge:delete", knowledgeId, servletRequest);
			required(knowledgeId, "knowledgeId");
			if (expectedVersion <= 0L) throw new IllegalArgumentException("expectedVersion must be positive");
			management().delete(tenantId, knowledgeId, expectedVersion);
			audit(tenantId, "knowledge:delete", knowledgeId, true, null);
		} catch (RuntimeException exception) {
			audit(tenantId, "knowledge:delete", knowledgeId, false, exception);
			throw exception;
		}
	}

	public TinyflowIngestionTask importText(TextImportRequest request, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		String knowledgeId = request == null ? null : request.getKnowledgeId();
		try {
			permissionEvaluator.check(tenantId, "knowledge:import", knowledgeId, servletRequest);
			validateImport(request);
			required(request.getName(), "name"); bounded(request.getName(), "name", MAX_NAME_LENGTH);
			nonBlank(request.getContent(), "content");
			byte[] content = request.getContent().getBytes(StandardCharsets.UTF_8);
			limit(content);
			TinyflowIngestionTask result = management().importDocument(command(tenantId, request, "text",
					request.getName(), "text/plain", content, null));
			audit(tenantId, "knowledge:import", knowledgeId, true, null);
			return result;
		} catch (RuntimeException exception) {
			audit(tenantId, "knowledge:import", knowledgeId, false, exception);
			throw exception;
		}
	}

	public TinyflowIngestionTask importFile(FileImportRequest request, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		String knowledgeId = request == null ? null : request.getKnowledgeId();
		try {
			permissionEvaluator.check(tenantId, "knowledge:import", knowledgeId, servletRequest);
			validateImport(request);
			required(request.getFilename(), "filename"); bounded(request.getFilename(), "filename", MAX_NAME_LENGTH);
			required(request.getMediaType(), "mediaType");
			limit(request.getContent());
			TinyflowIngestionTask result = management().importDocument(command(tenantId, request, "file",
					request.getFilename(), request.getMediaType(), request.getContent(), null));
			audit(tenantId, "knowledge:import", knowledgeId, true, null);
			return result;
		} catch (RuntimeException exception) {
			audit(tenantId, "knowledge:import", knowledgeId, false, exception);
			throw exception;
		}
	}

	public TinyflowIngestionTask importUrl(UrlImportRequest request, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		String knowledgeId = request == null ? null : request.getKnowledgeId();
		try {
			permissionEvaluator.check(tenantId, "knowledge:import", knowledgeId, servletRequest);
			validateImport(request); required(request.getUrl(), "url"); bounded(request.getUrl(), "url", MAX_URL_LENGTH);
			if (urlImporter == null) throw new IllegalStateException("URL importer is unavailable");
			MagicTinyflowSafeUrlImporter.ImportedContent imported;
			try {
				imported = urlImporter.fetch(request.getUrl());
			} catch (IOException exception) {
				throw new IllegalStateException("URL import failed", exception);
			}
			TinyflowIngestionTask result = management().importDocument(command(tenantId, request, "url",
					imported.getFilename(), imported.getMediaType(), imported.getContent(), imported.getFinalUrl()));
			audit(tenantId, "knowledge:import", knowledgeId, true, null);
			return result;
		} catch (RuntimeException exception) {
			audit(tenantId, "knowledge:import", knowledgeId, false, exception);
			throw exception;
		}
	}

	public PageResponse<TinyflowChunk> listChunks(String knowledgeId, String documentId, int page, int size,
			MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "chunk:list", knowledgeId, servletRequest);
			required(knowledgeId, "knowledgeId"); required(documentId, "documentId"); page(page, size);
			PageResponse<TinyflowChunk> result = response(management().listChunks(
					tenantId, knowledgeId, documentId, page, size));
			audit(tenantId, "chunk:list", knowledgeId, true, null);
			return result;
		} catch (RuntimeException exception) {
			audit(tenantId, "chunk:list", knowledgeId, false, exception);
			throw exception;
		}
	}

	public TinyflowChunk updateChunk(ChunkUpdateRequest request, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		String knowledgeId = request == null ? null : request.getKnowledgeId();
		String chunkId = request == null ? null : request.getChunkId();
		try {
			permissionEvaluator.check(tenantId, "chunk:update", chunkId, servletRequest);
			if (request == null) throw new IllegalArgumentException("request must not be null");
			required(knowledgeId, "knowledgeId"); required(chunkId, "chunkId"); nonBlank(request.getContent(), "content");
			limit(request.getContent().getBytes(StandardCharsets.UTF_8)); structured(request.getMetadata(), "metadata");
			TinyflowChunk result = management().updateChunk(tenantId, knowledgeId, chunkId,
					request.isEnabled(), request.getContent(), request.getMetadata());
			audit(tenantId, "chunk:update", chunkId, true, null);
			return result;
		} catch (RuntimeException exception) {
			audit(tenantId, "chunk:update", chunkId, false, exception);
			throw exception;
		}
	}

	public PageResponse<TinyflowIngestionTask> listTasks(String knowledgeId, int page, int size,
			MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "task:list", knowledgeId, servletRequest);
			required(knowledgeId, "knowledgeId"); page(page, size);
			PageResponse<TinyflowIngestionTask> result = response(management().listTasks(
					tenantId, knowledgeId, page, size));
			audit(tenantId, "task:list", knowledgeId, true, null);
			return result;
		} catch (RuntimeException exception) {
			audit(tenantId, "task:list", knowledgeId, false, exception);
			throw exception;
		}
	}

	public TinyflowIngestionTask retryTask(String knowledgeId, String taskId,
			MagicHttpServletRequest servletRequest) {
		return taskAction(knowledgeId, taskId, false, servletRequest);
	}

	public TinyflowIngestionTask cancelTask(String knowledgeId, String taskId,
			MagicHttpServletRequest servletRequest) {
		return taskAction(knowledgeId, taskId, true, servletRequest);
	}

	public TinyflowKnowledgeResult recall(RecallRequest request, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		String knowledgeId = request == null ? null : request.getKnowledgeId();
		try {
			permissionEvaluator.check(tenantId, "knowledge:recall", knowledgeId, servletRequest);
			if (request == null) throw new IllegalArgumentException("request must not be null");
			required(knowledgeId, "knowledgeId"); required(request.getQuery(), "query");
			bounded(request.getQuery(), "query", MAX_QUERY_LENGTH); structured(request.getOptions(), "options");
			if (request.getTopK() < 1 || request.getTopK() > MAX_PAGE_SIZE) {
				throw new IllegalArgumentException("topK must be between 1 and 100");
			}
			TinyflowKnowledgeRequest value = TinyflowKnowledgeRequest.builder().providerKey("fw-plus-local")
					.tenantId(tenantId).knowledgeId(knowledgeId).query(request.getQuery()).topK(request.getTopK())
					.threshold(request.getThreshold()).retrievalOptions(request.getOptions()).build();
			TinyflowKnowledgeResult result = management().retrieve(value);
			audit(tenantId, "knowledge:recall", knowledgeId, true, null);
			return result;
		} catch (RuntimeException exception) {
			audit(tenantId, "knowledge:recall", knowledgeId, false, exception);
			throw exception;
		}
	}

	public List<MagicTinyflowKnowledgeOptionProvider.Option> listModelOptions(int limit,
			MagicHttpServletRequest servletRequest) {
		return options("option:model", limit, servletRequest);
	}

	public List<MagicTinyflowKnowledgeOptionProvider.Option> listVectorOptions(int limit,
			MagicHttpServletRequest servletRequest) {
		return options("option:vector", limit, servletRequest);
	}

	public List<MagicTinyflowKnowledgeOptionProvider.Option> listS3Options(int limit,
			MagicHttpServletRequest servletRequest) {
		return options("option:s3", limit, servletRequest);
	}

	public CreatedKeyResponse createKey(CreateKeyRequest request, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		String knowledgeId = request == null ? null : request.getKnowledgeId();
		try {
			permissionEvaluator.check(tenantId, "key:create", knowledgeId, servletRequest);
			required(knowledgeId, "knowledgeId");
			TinyflowKnowledgeAccessKeyService.CreatedAccessKey created = accessKeyService.create(tenantId, knowledgeId);
			audit(tenantId, "key:create", knowledgeId, true, null);
			return new CreatedKeyResponse(created.getAccessKeyId(), created.getToken());
		} catch (RuntimeException exception) {
			audit(tenantId, "key:create", knowledgeId, false, exception);
			throw exception;
		}
	}

	public DifyConnectionView saveDifyConnection(DifyConnectionRequest request,
			MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		String connectionId = request == null ? null : request.getConnectionId();
		try {
			permissionEvaluator.check(tenantId, "dify:save", connectionId, servletRequest);
			validate(request);
			TinyflowDifyConnectionRepository.DifyConnection connection =
					new TinyflowDifyConnectionRepository.DifyConnection(connectionId, tenantId,
							request.getKnowledgeId(), request.getBaseUrl(), request.getDatasetId(), request.getCredentialRef());
			repository.save(connection);
			audit(tenantId, "dify:save", connectionId, true, null);
			return new DifyConnectionView(connection);
		} catch (RuntimeException exception) {
			audit(tenantId, "dify:save", connectionId, false, exception);
			throw exception;
		}
	}

	public List<AccessKeyView> listKeys(String knowledgeId, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "key:list", knowledgeId, servletRequest);
			required(knowledgeId, "knowledgeId");
			List<AccessKeyView> result = new ArrayList<AccessKeyView>();
			for (TinyflowKnowledgeAccessKeyRepository.AccessKey value : repository.findAccessKeys(tenantId, knowledgeId)) {
				result.add(new AccessKeyView(value));
				if (result.size() == MAX_SECURITY_LIST_SIZE) break;
			}
			audit(tenantId, "key:list", knowledgeId, true, null);
			return Collections.unmodifiableList(result);
		} catch (RuntimeException exception) {
			audit(tenantId, "key:list", knowledgeId, false, exception);
			throw exception;
		}
	}

	public void revokeKey(String knowledgeId, String accessKeyId, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "key:revoke", accessKeyId, servletRequest);
			required(knowledgeId, "knowledgeId"); required(accessKeyId, "accessKeyId");
			repository.delete(tenantId, knowledgeId, accessKeyId);
			audit(tenantId, "key:revoke", accessKeyId, true, null);
		} catch (RuntimeException exception) {
			audit(tenantId, "key:revoke", accessKeyId, false, exception);
			throw exception;
		}
	}

	public DifyConnectionView getDifyConnection(String connectionId, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "dify:view", connectionId, servletRequest);
			required(connectionId, "connectionId");
			TinyflowDifyConnectionRepository.DifyConnection value = repository.find(tenantId, connectionId);
			audit(tenantId, "dify:view", connectionId, true, null);
			return value == null ? null : new DifyConnectionView(value);
		} catch (RuntimeException exception) {
			audit(tenantId, "dify:view", connectionId, false, exception);
			throw exception;
		}
	}

	public List<DifyConnectionView> listDifyConnections(String knowledgeId,
			MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "dify:list", knowledgeId, servletRequest);
			required(knowledgeId, "knowledgeId");
			List<DifyConnectionView> result = new ArrayList<DifyConnectionView>();
			for (TinyflowDifyConnectionRepository.DifyConnection value : repository.findByKnowledge(tenantId, knowledgeId)) {
				result.add(new DifyConnectionView(value));
				if (result.size() == MAX_SECURITY_LIST_SIZE) break;
			}
			audit(tenantId, "dify:list", knowledgeId, true, null);
			return Collections.unmodifiableList(result);
		} catch (RuntimeException exception) {
			audit(tenantId, "dify:list", knowledgeId, false, exception);
			throw exception;
		}
	}

	public void deleteDifyConnection(String connectionId, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "dify:delete", connectionId, servletRequest);
			required(connectionId, "connectionId");
			repository.delete(tenantId, connectionId);
			audit(tenantId, "dify:delete", connectionId, true, null);
		} catch (RuntimeException exception) {
			audit(tenantId, "dify:delete", connectionId, false, exception);
			throw exception;
		}
	}

	public void testDifyConnection(String connectionId, MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, "dify:test", connectionId, servletRequest);
			required(connectionId, "connectionId");
			TinyflowDifyConnectionRepository.DifyConnection value = repository.find(tenantId, connectionId);
			if (value == null) throw new IllegalArgumentException("Dify connection not found");
			if (difyConnectionTester == null) throw new IllegalStateException("Dify connection tester is unavailable");
			difyConnectionTester.test(value);
			audit(tenantId, "dify:test", connectionId, true, null);
		} catch (RuntimeException exception) {
			audit(tenantId, "dify:test", connectionId, false, exception);
			throw exception;
		}
	}

	private MagicTinyflowKnowledgeOperations management() {
		if (operations == null) throw new IllegalStateException("Knowledge management operations are unavailable");
		return operations;
	}

	private TinyflowKnowledgeDefinition definition(String tenantId, String knowledgeId, KnowledgeRequest request) {
		return TinyflowKnowledgeDefinition.builder().providerKey("fw-plus-local").tenantId(tenantId)
				.knowledgeId(knowledgeId).name(request.getName()).description(request.getDescription())
				.metadata(request.getMetadata()).enabled(request.isEnabled()).build();
	}

	private void validateKnowledge(KnowledgeRequest request, boolean update) {
		if (request == null) throw new IllegalArgumentException("request must not be null");
		required(request.getKnowledgeId(), "knowledgeId"); required(request.getName(), "name");
		bounded(request.getName(), "name", MAX_NAME_LENGTH);
		bounded(request.getDescription(), "description", MAX_REQUEST_STRING_LENGTH);
		structured(request.getMetadata(), "metadata");
		if (update && request.getExpectedVersion() <= 0L) {
			throw new IllegalArgumentException("expectedVersion must be positive");
		}
	}

	private void validateImport(BaseImportRequest request) {
		if (request == null) throw new IllegalArgumentException("request must not be null");
		required(request.getKnowledgeId(), "knowledgeId"); required(request.getSourceId(), "sourceId");
		structured(request.getOptions(), "options");
	}

	private MagicTinyflowKnowledgeOperations.ImportCommand command(String tenantId, BaseImportRequest request,
			String sourceType, String name, String mediaType, byte[] content, String sourceUri) {
		return new MagicTinyflowKnowledgeOperations.ImportCommand(tenantId, request.getKnowledgeId(),
				request.getSourceId(), sourceType, name, mediaType, content, sourceUri, request.getOptions());
	}

	private void limit(byte[] content) {
		if (content == null) throw new IllegalArgumentException("content must not be null");
		if (content.length > MAX_IMPORT_BYTES) throw new IllegalArgumentException("content exceeds size limit");
	}

	private void page(int page, int size) {
		if (page < 1) throw new IllegalArgumentException("page must be at least 1");
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("size must be between 1 and 100");
		}
	}

	private <T> PageResponse<T> response(MagicTinyflowKnowledgeOperations.PageResult<T> source) {
		return new PageResponse<T>(source.getPage(), source.getSize(), source.getTotal(), source.getItems());
	}

	private TinyflowIngestionTask taskAction(String knowledgeId, String taskId, boolean cancel,
			MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		String action = cancel ? "task:cancel" : "task:retry";
		try {
			permissionEvaluator.check(tenantId, action, taskId, servletRequest);
			required(knowledgeId, "knowledgeId"); required(taskId, "taskId");
			TinyflowIngestionTask result = cancel ? management().cancel(tenantId, knowledgeId, taskId)
					: management().retry(tenantId, knowledgeId, taskId);
			audit(tenantId, action, taskId, true, null);
			return result;
		} catch (RuntimeException exception) {
			audit(tenantId, action, taskId, false, exception);
			throw exception;
		}
	}

	private List<MagicTinyflowKnowledgeOptionProvider.Option> options(String action, int limit,
			MagicHttpServletRequest servletRequest) {
		String tenantId = tenant(servletRequest);
		try {
			permissionEvaluator.check(tenantId, action, null, servletRequest);
			if (limit < 1 || limit > MAX_OPTION_LIMIT) {
				throw new IllegalArgumentException("limit must be between 1 and 200");
			}
			if (optionProvider == null) throw new IllegalStateException("Knowledge options are unavailable");
			List<MagicTinyflowKnowledgeOptionProvider.Option> source;
			if ("option:model".equals(action)) source = optionProvider.listModels(limit);
			else if ("option:vector".equals(action)) source = optionProvider.listVectors(limit);
			else source = optionProvider.listS3(limit);
			List<MagicTinyflowKnowledgeOptionProvider.Option> result = source == null
					? Collections.<MagicTinyflowKnowledgeOptionProvider.Option>emptyList()
					: new ArrayList<MagicTinyflowKnowledgeOptionProvider.Option>(source);
			if (result.size() > limit) result = new ArrayList<MagicTinyflowKnowledgeOptionProvider.Option>(result.subList(0, limit));
			audit(tenantId, action, null, true, null);
			return Collections.unmodifiableList(result);
		} catch (RuntimeException exception) {
			audit(tenantId, action, null, false, exception);
			throw exception;
		}
	}

	private String tenant(MagicHttpServletRequest request) {
		String tenantId = tenantResolver.resolve(request);
		required(tenantId, "tenantId");
		return tenantId.trim();
	}

	private void validate(DifyConnectionRequest request) {
		if (request == null) throw new IllegalArgumentException("request must not be null");
		required(request.getConnectionId(), "connectionId"); required(request.getKnowledgeId(), "knowledgeId");
		required(request.getBaseUrl(), "baseUrl"); required(request.getDatasetId(), "datasetId");
		required(request.getCredentialRef(), "credentialRef");
		bounded(request.getBaseUrl(), "baseUrl", MAX_URL_LENGTH);
		bounded(request.getApiKey(), "apiKey", MAX_STRUCTURED_STRING_LENGTH);
	}

	private void required(String value, String field) {
		nonBlank(value, field); bounded(value, field, MAX_REQUEST_STRING_LENGTH);
	}

	private void nonBlank(String value, String field) {
		if (value == null || value.trim().length() == 0) throw new IllegalArgumentException(field + " must not be blank");
	}

	private void bounded(String value, String field, int limit) {
		if (value != null && value.length() > limit) {
			throw new IllegalArgumentException(field + " exceeds length limit");
		}
	}

	private void structured(Object value, String field) {
		structured(value, field, 0, new IdentityHashMap<Object, Boolean>());
	}

	private void structured(Object value, String field, int depth, IdentityHashMap<Object, Boolean> visiting) {
		if (value == null) return;
		if (depth > MAX_STRUCTURED_DEPTH) throw new IllegalArgumentException(field + " exceeds depth limit");
		if (value instanceof CharSequence) {
			bounded(value.toString(), field, MAX_STRUCTURED_STRING_LENGTH); return;
		}
		if (value instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) value;
			if (map.size() > MAX_STRUCTURED_ENTRIES) throw new IllegalArgumentException(field + " exceeds entry limit");
			enter(value, field, visiting);
			try {
				for (Map.Entry<?, ?> entry : map.entrySet()) {
					if (!(entry.getKey() instanceof String)) {
						throw new IllegalArgumentException(field + " keys must be strings");
					}
					bounded((String) entry.getKey(), field + " key", MAX_NAME_LENGTH);
					structured(entry.getValue(), field, depth + 1, visiting);
				}
			} finally { visiting.remove(value); }
			return;
		}
		if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>) value;
			if (collection.size() > MAX_STRUCTURED_ENTRIES) throw new IllegalArgumentException(field + " exceeds entry limit");
			enter(value, field, visiting);
			try {
				for (Object item : collection) structured(item, field, depth + 1, visiting);
			} finally { visiting.remove(value); }
			return;
		}
		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			if (length > MAX_STRUCTURED_ENTRIES) throw new IllegalArgumentException(field + " exceeds entry limit");
			enter(value, field, visiting);
			try {
				for (int index = 0; index < length; index++) {
					structured(Array.get(value, index), field, depth + 1, visiting);
				}
			} finally { visiting.remove(value); }
		}
	}

	private void enter(Object value, String field, IdentityHashMap<Object, Boolean> visiting) {
		if (visiting.put(value, Boolean.TRUE) != null) {
			throw new IllegalArgumentException(field + " must not contain cycles");
		}
	}

	private void audit(String tenantId, String action, String resourceId, boolean success, RuntimeException error) {
		if (!success && error instanceof AuditPersistenceException) return;
		try {
			auditSink.record(new MagicTinyflowKnowledgeAuditSink.AuditEvent(tenantId, action, resourceId, success,
					error == null ? null : error.getClass().getName()));
		} catch (RuntimeException auditFailure) {
			if (error == null) throw new AuditPersistenceException(auditFailure);
			if (auditFailure != error) error.addSuppressed(auditFailure);
		}
	}

	private static final class AuditPersistenceException extends IllegalStateException {
		private AuditPersistenceException(RuntimeException cause) {
			super(cause.getMessage(), cause);
		}
	}

	public static final class KnowledgeView {
		private final String providerKey;
		private final String knowledgeId;
		private final String name;
		private final String description;
		private final Map<String, Object> metadata;
		private final boolean enabled;
		private final long version;

		KnowledgeView(MagicTinyflowKnowledgeOperations.KnowledgeRecord record) {
			TinyflowKnowledgeDefinition value = record.getDefinition();
			this.providerKey = value.getProviderKey(); this.knowledgeId = value.getKnowledgeId();
			this.name = value.getName(); this.description = value.getDescription();
			this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value.getMetadata()));
			this.enabled = value.isEnabled(); this.version = record.getVersion();
		}

		public String getProviderKey() { return providerKey; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getName() { return name; }
		public String getDescription() { return description; }
		public Map<String, Object> getMetadata() { return metadata; }
		public boolean isEnabled() { return enabled; }
		public long getVersion() { return version; }
	}

	public static final class PageResponse<T> {
		private final int page;
		private final int size;
		private final long total;
		private final List<T> items;

		PageResponse(int page, int size, long total, List<T> items) {
			this.page = page; this.size = size; this.total = total;
			this.items = items == null ? Collections.<T>emptyList()
					: Collections.unmodifiableList(new ArrayList<T>(items));
		}

		public int getPage() { return page; }
		public int getSize() { return size; }
		public long getTotal() { return total; }
		public List<T> getItems() { return items; }
	}

	public static class KnowledgeRequest {
		private String tenantId;
		private String knowledgeId;
		private String name;
		private String description;
		private Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		private boolean enabled = true;
		private long expectedVersion;
		public String getTenantId() { return tenantId; }
		public void setTenantId(String tenantId) { this.tenantId = tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public String getDescription() { return description; }
		public void setDescription(String description) { this.description = description; }
		public Map<String, Object> getMetadata() { return metadata; }
		public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
		public long getExpectedVersion() { return expectedVersion; }
		public void setExpectedVersion(long expectedVersion) { this.expectedVersion = expectedVersion; }
	}

	public static class BaseImportRequest {
		private String tenantId;
		private String knowledgeId;
		private String sourceId;
		private String name;
		private Map<String, Object> options = new LinkedHashMap<String, Object>();
		public String getTenantId() { return tenantId; }
		public void setTenantId(String tenantId) { this.tenantId = tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
		public String getSourceId() { return sourceId; }
		public void setSourceId(String sourceId) { this.sourceId = sourceId; }
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public Map<String, Object> getOptions() { return options; }
		public void setOptions(Map<String, Object> options) { this.options = options; }
	}

	public static class TextImportRequest extends BaseImportRequest {
		private String content;
		public String getContent() { return content; }
		public void setContent(String content) { this.content = content; }
	}

	public static class FileImportRequest extends BaseImportRequest {
		private String filename;
		private String mediaType;
		private byte[] content;
		public String getFilename() { return filename; }
		public void setFilename(String filename) { this.filename = filename; }
		public String getMediaType() { return mediaType; }
		public void setMediaType(String mediaType) { this.mediaType = mediaType; }
		public byte[] getContent() { return content == null ? null : content.clone(); }
		public void setContent(byte[] content) { this.content = content == null ? null : content.clone(); }
	}

	public static class UrlImportRequest extends BaseImportRequest {
		private String url;
		public String getUrl() { return url; }
		public void setUrl(String url) { this.url = url; }
	}

	public static class ChunkUpdateRequest {
		private String tenantId;
		private String knowledgeId;
		private String chunkId;
		private boolean enabled = true;
		private String content;
		private Map<String, Object> metadata;
		public String getTenantId() { return tenantId; }
		public void setTenantId(String tenantId) { this.tenantId = tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
		public String getChunkId() { return chunkId; }
		public void setChunkId(String chunkId) { this.chunkId = chunkId; }
		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
		public String getContent() { return content; }
		public void setContent(String content) { this.content = content; }
		public Map<String, Object> getMetadata() { return metadata; }
		public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
	}

	public static class RecallRequest {
		private String tenantId;
		private String knowledgeId;
		private String query;
		private int topK = 10;
		private Double threshold;
		private Map<String, Object> options = new LinkedHashMap<String, Object>();
		public String getTenantId() { return tenantId; }
		public void setTenantId(String tenantId) { this.tenantId = tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
		public String getQuery() { return query; }
		public void setQuery(String query) { this.query = query; }
		public int getTopK() { return topK; }
		public void setTopK(int topK) { this.topK = topK; }
		public Double getThreshold() { return threshold; }
		public void setThreshold(Double threshold) { this.threshold = threshold; }
		public Map<String, Object> getOptions() { return options; }
		public void setOptions(Map<String, Object> options) { this.options = options; }
	}

	public static final class CreatedKeyResponse {
		private final String accessKeyId;
		private final String token;
		CreatedKeyResponse(String accessKeyId, String token) { this.accessKeyId = accessKeyId; this.token = token; }
		public String getAccessKeyId() { return accessKeyId; }
		public String getToken() { return token; }
	}

	public static final class AccessKeyView {
		private final String accessKeyId;
		private final String prefix;
		private final boolean enabled;
		AccessKeyView(TinyflowKnowledgeAccessKeyRepository.AccessKey value) {
			this.accessKeyId = value.getAccessKeyId(); this.prefix = value.getPrefix(); this.enabled = value.isEnabled();
		}
		public String getAccessKeyId() { return accessKeyId; }
		public String getPrefix() { return prefix; }
		public boolean isEnabled() { return enabled; }
		@Override public String toString() {
			return "AccessKeyView{accessKeyId='" + accessKeyId + "', prefix='" + prefix + "', enabled=" + enabled + "}";
		}
	}

	public static class CreateKeyRequest {
		private String tenantId;
		private String knowledgeId;
		public String getTenantId() { return tenantId; }
		public void setTenantId(String tenantId) { this.tenantId = tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
	}

	public static class DifyConnectionRequest {
		private String tenantId;
		private String connectionId;
		private String knowledgeId;
		private String baseUrl;
		private String datasetId;
		private String credentialRef;
		private transient String apiKey;
		public String getTenantId() { return tenantId; }
		public void setTenantId(String tenantId) { this.tenantId = tenantId; }
		public String getConnectionId() { return connectionId; }
		public void setConnectionId(String connectionId) { this.connectionId = connectionId; }
		public String getKnowledgeId() { return knowledgeId; }
		public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
		public String getBaseUrl() { return baseUrl; }
		public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
		public String getDatasetId() { return datasetId; }
		public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
		public String getCredentialRef() { return credentialRef; }
		public void setCredentialRef(String credentialRef) { this.credentialRef = credentialRef; }
		public String getApiKey() { return apiKey; }
		public void setApiKey(String apiKey) { this.apiKey = apiKey; }
	}

	public static final class DifyConnectionView {
		private final String connectionId;
		private final String knowledgeId;
		private final String baseUrl;
		private final String datasetId;
		private final String credentialRef;
		DifyConnectionView(TinyflowDifyConnectionRepository.DifyConnection value) {
			this.connectionId = value.getConnectionId(); this.knowledgeId = value.getKnowledgeId();
			this.baseUrl = value.getBaseUrl(); this.datasetId = value.getDatasetId(); this.credentialRef = value.getCredentialRef();
		}
		public String getConnectionId() { return connectionId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getBaseUrl() { return baseUrl; }
		public String getDatasetId() { return datasetId; }
		public String getCredentialRef() { return credentialRef; }
	}

	public interface DifyConnectionTester {
		void test(TinyflowDifyConnectionRepository.DifyConnection connection);
	}
}
