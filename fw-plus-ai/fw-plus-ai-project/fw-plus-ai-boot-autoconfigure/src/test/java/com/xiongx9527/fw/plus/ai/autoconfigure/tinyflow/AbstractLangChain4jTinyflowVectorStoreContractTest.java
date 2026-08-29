package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider.InMemoryEmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.DefaultEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepositoryContractTest.InMemoryTinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.local.index.TinyflowLocalIndexer;
import com.xiongx9527.fw.plus.ai.tinyflow.local.index.TinyflowVectorCompensator;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.AbstractTinyflowVectorStoreContractTest;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.Collections;
import java.util.List;

public abstract class AbstractLangChain4jTinyflowVectorStoreContractTest
		extends AbstractTinyflowVectorStoreContractTest {

	protected static final String STORE_KEY = "contract-store";
	private static final String TENANT_ID = "tenant-contract";
	private static final String KNOWLEDGE_ID = "knowledge-contract";
	private static final String SOURCE_ID = "source-contract";
	private static final String DOCUMENT_ID = "document-contract";

	protected abstract ProviderFixture createProviderFixture();

	@Override
	protected final VectorStoreFixture createFixture() {
		final ProviderFixture providerFixture = createProviderFixture();
		return new VectorStoreFixture() {
			@Override
			public TinyflowVectorStore getStore() {
				return providerFixture.provider.get(STORE_KEY);
			}

			@Override
			public void addVector(String id) {
				providerFixture.embeddingStore.add(new AiEmbeddingDocument(id, null,
						Collections.<String, Object>emptyMap(), new AiEmbeddingVector(new float[] { 1.0f })));
			}

			@Override
			public boolean containsVector(String id) {
				AiEmbeddingSearchResult result = providerFixture.embeddingStore.search(
						new AiEmbeddingQuery(new AiEmbeddingVector(new float[] { 1.0f }), 10, 0.0));
				for (AiEmbeddingMatch match : result.getMatches()) {
					if (id.equals(match.getEmbeddingId())) {
						return true;
					}
				}
				return false;
			}

			@Override
			public StageRetryScenario createDeletePendingStageRetry(String vectorId) {
				return stageRetryScenario(providerFixture.provider, vectorId);
			}
		};
	}

	protected final AiEmbeddingStore createLangChain4jInMemoryStore() {
		AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
		definition.setKey(STORE_KEY);
		definition.setFramework("langchain4j");
		definition.setProvider("in-memory");
		definition.setType(AiEmbeddingStoreType.VECTOR);
		definition.setEnabled(true);
		return new DefaultEmbeddingStoreFactory(Collections.singletonList(
				new InMemoryEmbeddingStoreProviderAdapter())).create(definition);
	}

	private StageRetryScenario stageRetryScenario(TinyflowVectorStoreProvider provider, String vectorId) {
		final InMemoryTinyflowKnowledgeRepository repository = new InMemoryTinyflowKnowledgeRepository();
		TinyflowDocument document = TinyflowDocument.builder().tenantId(TENANT_ID).knowledgeId(KNOWLEDGE_ID)
				.sourceId(SOURCE_ID).sourceType("file").documentId(DOCUMENT_ID).checksum("checksum-contract")
				.name("contract.txt").build();
		TinyflowChunk chunk = TinyflowChunk.builder().tenantId(TENANT_ID).knowledgeId(KNOWLEDGE_ID)
				.sourceId(SOURCE_ID).sourceType("file").documentId(DOCUMENT_ID).chunkId("chunk-contract")
				.content("contract").vectorIds(Collections.singletonList(vectorId)).build();
		repository.saveDocument(document);
		repository.saveChunks(Collections.singletonList(chunk));
		repository.activateDocumentVersion(TENANT_ID, KNOWLEDGE_ID, SOURCE_ID, DOCUMENT_ID);
		repository.saveVectorCompensation(new TinyflowKnowledgeRepository.VectorCompensation(
				"compensation-contract", TENANT_ID, KNOWLEDGE_ID, SOURCE_ID, DOCUMENT_ID,
				Collections.singletonList(vectorId), "DELETE_PENDING"));

		return new StageRetryScenario() {
			@Override
			public void runUntilStageCasFails() {
				repository.failNextStageAdvance();
				new TinyflowVectorCompensator(repository, indexer(repository, provider))
						.retry(TENANT_ID, KNOWLEDGE_ID);
			}

			@Override
			public void retryWithNewCompensator() {
				new TinyflowVectorCompensator(repository, indexer(repository, provider))
						.retry(TENANT_ID, KNOWLEDGE_ID);
			}

			@Override
			public boolean isResourcePresent() {
				return !repository.findDocumentVersions(TENANT_ID, KNOWLEDGE_ID, SOURCE_ID).isEmpty()
						|| !repository.findChunksByDocument(TENANT_ID, KNOWLEDGE_ID, DOCUMENT_ID).isEmpty();
			}

			@Override
			public boolean isVectorStagePending() {
				List<TinyflowKnowledgeRepository.VectorCompensation> compensations =
						repository.findVectorCompensations(TENANT_ID, KNOWLEDGE_ID);
				return compensations.size() == 1
						&& TinyflowKnowledgeRepository.VectorCompensation.VECTOR_PENDING.equals(
								compensations.get(0).getStage());
			}

			@Override
			public boolean isCompensationPresent() {
				return !repository.findVectorCompensations(TENANT_ID, KNOWLEDGE_ID).isEmpty();
			}

			@Override
			public boolean isDeletedTaskPersisted() {
				for (TinyflowIngestionTask task : repository.findTasks(TENANT_ID, KNOWLEDGE_ID)) {
					if ("DELETED".equals(task.getStatus())) {
						return true;
					}
				}
				return false;
			}
		};
	}

	private TinyflowLocalIndexer indexer(InMemoryTinyflowKnowledgeRepository repository,
			TinyflowVectorStoreProvider provider) {
		return new TinyflowLocalIndexer(repository, new TinyflowModelProvider() {
			@Override public List<TinyflowModelDescriptor> listChatModels() { return Collections.emptyList(); }
			@Override public List<TinyflowModelDescriptor> listEmbeddingModels() { return Collections.emptyList(); }
			@Override public ChatLanguageModel getChatModel(String key) { return null; }
			@Override public TinyflowEmbeddingModel getEmbeddingModel(String key) {
				return text -> new float[] { 1.0f };
			}
		}, provider, "contract-model", STORE_KEY, 10);
	}

	protected static final class ProviderFixture {
		private final AiEmbeddingStore embeddingStore;
		private final TinyflowVectorStoreProvider provider;

		public ProviderFixture(AiEmbeddingStore embeddingStore, TinyflowVectorStoreProvider provider) {
			this.embeddingStore = embeddingStore;
			this.provider = provider;
		}
	}
}
