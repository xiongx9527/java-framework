package com.xiongx9527.fw.plus.ai.tinyflow.local;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentImport;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeCapabilities;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeManagementProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval.TinyflowLocalRetriever;

public final class FwPlusLocalKnowledgeProvider implements TinyflowKnowledgeManagementProvider {

	private final TinyflowKnowledgeRepository repository;
	private final DefaultTinyflowIngestionService ingestionService;
	private final TinyflowLocalRetriever retriever;

	public FwPlusLocalKnowledgeProvider(TinyflowKnowledgeRepository repository,
			DefaultTinyflowIngestionService ingestionService) {
		if (repository == null || ingestionService == null) {
			throw new IllegalArgumentException("repository and ingestionService must not be null");
		}
		this.repository = repository;
		this.ingestionService = ingestionService;
		this.retriever = null;
	}

	public FwPlusLocalKnowledgeProvider(TinyflowKnowledgeRepository repository, TinyflowLocalRetriever retriever) {
		if (repository == null || retriever == null) {
			throw new IllegalArgumentException("repository and retriever must not be null");
		}
		this.repository = repository;
		this.ingestionService = null;
		this.retriever = retriever;
	}

	public FwPlusLocalKnowledgeProvider(TinyflowKnowledgeRepository repository,
			DefaultTinyflowIngestionService ingestionService, TinyflowLocalRetriever retriever) {
		if (repository == null || ingestionService == null || retriever == null) {
			throw new IllegalArgumentException("repository, ingestionService and retriever must not be null");
		}
		this.repository = repository;
		this.ingestionService = ingestionService;
		this.retriever = retriever;
	}

	@Override
	public String getKey() {
		return DefaultTinyflowIngestionService.PROVIDER_KEY;
	}

	@Override
	public TinyflowKnowledgeCapabilities capabilities() {
		return TinyflowKnowledgeCapabilities.of(retriever != null, true, ingestionService != null);
	}

	@Override
	public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
		if (retriever != null) {
			return retriever.retrieve(request);
		}
		if (request == null || repository.findKnowledge(request.getTenantId(), request.getKnowledgeId()) == null) {
			return TinyflowKnowledgeResult.notFound();
		}
		return TinyflowKnowledgeResult.empty();
	}

	@Override
	public TinyflowKnowledgeDefinition create(TinyflowKnowledgeDefinition definition) {
		validate(definition);
		if (repository.findKnowledge(definition.getTenantId(), definition.getKnowledgeId()) != null) {
			throw new IllegalArgumentException("Knowledge already exists");
		}
		repository.saveKnowledge(definition, 0L);
		return definition;
	}

	@Override
	public TinyflowKnowledgeDefinition update(TinyflowKnowledgeDefinition definition) {
		validate(definition);
		if (repository.findKnowledge(definition.getTenantId(), definition.getKnowledgeId()) == null) {
			throw new IllegalArgumentException("Knowledge does not exist");
		}
		long version = repository.findKnowledgeVersion(definition.getTenantId(), definition.getKnowledgeId());
		repository.saveKnowledge(definition, version);
		return definition;
	}

	@Override
	public void delete(String tenantId, String knowledgeId) {
		long version = repository.findKnowledgeVersion(tenantId, knowledgeId);
		if (version == 0L) {
			return;
		}
		repository.deleteKnowledge(tenantId, knowledgeId, version);
	}

	@Override
	public TinyflowIngestionTask importDocument(TinyflowDocumentImport request) {
		if (ingestionService == null) {
			throw new IllegalStateException("Ingestion service is unavailable");
		}
		return ingestionService.importDocument(request);
	}

	private void validate(TinyflowKnowledgeDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("definition must not be null");
		}
		if (!getKey().equals(definition.getProviderKey())) {
			throw new IllegalArgumentException("providerKey must be fw-plus-local");
		}
	}
}
