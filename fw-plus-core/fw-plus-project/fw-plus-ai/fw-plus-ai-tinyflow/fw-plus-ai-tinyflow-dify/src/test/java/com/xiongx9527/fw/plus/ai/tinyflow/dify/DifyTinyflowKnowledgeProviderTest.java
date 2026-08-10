package com.xiongx9527.fw.plus.ai.tinyflow.dify;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowCredentialResolver;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDifyConnectionRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.dataset.entity.Document;
import com.xiongx9527.fw.plus.dify.dataset.entity.Segment;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrieveResult;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrievalModel;
import com.xiongx9527.fw.plus.dify.types.DifyClientException;
import com.xiongx9527.fw.plus.dify.types.DifyException;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DifyTinyflowKnowledgeProviderTest {

	@Test
	public void shouldMapRequestAndResponseToStableSourceIdentity() {
		AtomicReference<String> tenantId = new AtomicReference<String>();
		AtomicReference<String> knowledgeId = new AtomicReference<String>();
		AtomicReference<String> datasetId = new AtomicReference<String>();
		AtomicReference<String> query = new AtomicReference<String>();
		AtomicReference<RetrievalModel> retrievalModel = new AtomicReference<RetrievalModel>();
		IDatasetClient client = datasetClient(retrieveResult(), datasetId, query, retrievalModel, null);
		DifyTinyflowKnowledgeProvider provider = provider(connectionRepository(connection(), tenantId, knowledgeId), client);

		TinyflowKnowledgeResult result = provider.retrieve(request("hello", 8, 0.62D));

		assertEquals("tenant-1", tenantId.get());
		assertEquals("knowledge-1", knowledgeId.get());
		assertEquals("dataset-7", datasetId.get());
		assertEquals("hello", query.get());
		assertEquals(Integer.valueOf(8), retrievalModel.get().getTopK());
		assertEquals(Double.valueOf(0.62D), retrievalModel.get().getScoreThreshold());
		assertEquals(Boolean.TRUE, retrievalModel.get().getScoreThresholdEnabled());
		assertEquals(1, result.getMatches().size());
		assertEquals("external-source-7", result.getMatches().get(0).getSourceId());
		assertEquals("notion", result.getMatches().get(0).getSourceType());
		assertEquals("document-7", result.getMatches().get(0).getDocumentId());
		assertEquals("segment-7", result.getMatches().get(0).getChunkId());
		assertEquals("retrieved content", result.getMatches().get(0).getContent());
		assertEquals(0.93D, result.getMatches().get(0).getScore(), 0D);
	}

	@Test
	public void shouldBeRetrievalOnlyAndNeverLeakApiKey() {
		IDatasetClient client = datasetClient(null, null, null, null,
				new DifyClientException("Authorization: Bearer secret-api-key"));
		DifyTinyflowKnowledgeProvider provider = provider(connectionRepository(connection(), null, null), client);

		assertTrue(provider.capabilities().supportsRetrieval());
		assertFalse(provider.capabilities().supportsManagement());
		assertFalse(provider.capabilities().supportsDocumentImport());
		TinyflowException error = assertThrows(TinyflowException.class, () -> provider.retrieve(request("hello", 1, null)));
		assertFalse(error.getMessage().contains("secret-api-key"));
		assertEquals("Dify retrieval failed", error.getMessage());
	}

	@Test
	public void shouldConvertUnauthorizedDifyResponseWithoutLeakingPayload() {
		IDatasetClient client = datasetClient(null, null, null, null,
				new DifyException("{\"message\":\"secret-api-key\"}", 401));
		DifyTinyflowKnowledgeProvider provider = provider(connectionRepository(connection(), null, null), client);

		TinyflowException error = assertThrows(TinyflowException.class, () -> provider.retrieve(request("hello", 1, null)));

		assertEquals("Dify access denied", error.getMessage());
		assertFalse(error.getMessage().contains("secret-api-key"));
	}

	@Test
	public void shouldConvertNotFoundAndTimeoutDifyResponses() {
		DifyTinyflowKnowledgeProvider notFound = provider(connectionRepository(connection(), null, null), datasetClient(null,
				null, null, null, new DifyException("{\"message\":\"secret-api-key\"}", 404)));
		TinyflowException notFoundError = assertThrows(TinyflowException.class,
				() -> notFound.retrieve(request("hello", 1, null)));
		assertEquals("Dify resource not found", notFoundError.getMessage());

		DifyTinyflowKnowledgeProvider timeout = provider(connectionRepository(connection(), null, null), datasetClient(null,
				null, null, null, new DifyClientException("secret-api-key", new SocketTimeoutException("read timed out"))));
		TinyflowException timeoutError = assertThrows(TinyflowException.class,
				() -> timeout.retrieve(request("hello", 1, null)));
		assertEquals("Dify retrieval timed out", timeoutError.getMessage());
		assertFalse(timeoutError.getMessage().contains("secret-api-key"));
	}

	@Test
	public void shouldUseStableDifyFallbackWhenSourceMetadataIsMissing() {
		Segment segment = Segment.builder().id("segment-fallback").documentId("document-fallback")
				.content("fallback content").build();
		RetrieveResult.Record record = new RetrieveResult.Record();
		record.setScore(0.5D);
		record.setSegment(segment);
		RetrieveResult response = new RetrieveResult();
		response.setRecords(Collections.singletonList(record));
		DifyTinyflowKnowledgeProvider provider = provider(connectionRepository(connection(), null, null),
				datasetClient(response, null, null, null, null));

		TinyflowKnowledgeResult result = provider.retrieve(request("hello", 1, null));

		assertEquals("document-fallback", result.getMatches().get(0).getSourceId());
		assertEquals("dify", result.getMatches().get(0).getSourceType());
	}

	@Test
	public void shouldRejectMissingAndAmbiguousKnowledgeConnections() {
		DifyTinyflowKnowledgeProvider missing = provider(connectionRepository(Collections.<TinyflowDifyConnectionRepository.DifyConnection>emptyList(), null, null),
				datasetClient(null, null, null, null, null));
		TinyflowException missingError = assertThrows(TinyflowException.class, () -> missing.retrieve(request("hello", 1, null)));
		assertEquals("Dify connection not found", missingError.getMessage());

		List<TinyflowDifyConnectionRepository.DifyConnection> ambiguousConnections = Arrays.asList(connection(),
				new TinyflowDifyConnectionRepository.DifyConnection("conn-2", "tenant-1", "knowledge-1",
						"https://dify.example", "dataset-8", "credential-2"));
		DifyTinyflowKnowledgeProvider ambiguous = provider(connectionRepository(ambiguousConnections, null, null),
				datasetClient(null, null, null, null, null));
		TinyflowException ambiguousError = assertThrows(TinyflowException.class, () -> ambiguous.retrieve(request("hello", 1, null)));
		assertEquals("Dify connection is ambiguous", ambiguousError.getMessage());
	}

	private DifyTinyflowKnowledgeProvider provider(TinyflowDifyConnectionRepository connections, final IDatasetClient client) {
		TinyflowCredentialResolver credentials = (tenantId, credentialRef) ->
				new TinyflowCredentialResolver.ResolvedCredential("secret-api-key".toCharArray(), Long.MAX_VALUE);
		DifyDatasetClientFactory factory = new DifyDatasetClientFactory(credentials, (baseUrl, apiKey) -> client);
		return new DifyTinyflowKnowledgeProvider(connections, factory);
	}

	private TinyflowDifyConnectionRepository connectionRepository(final List<TinyflowDifyConnectionRepository.DifyConnection> connections,
			AtomicReference<String> tenantId, AtomicReference<String> knowledgeId) {
		return new TinyflowDifyConnectionRepository() {
			@Override
			public DifyConnection find(String requestedTenantId, String connectionId) {
				return null;
			}

			@Override
			public List<DifyConnection> findByKnowledge(String requestedTenantId, String requestedKnowledgeId) {
				if (tenantId != null) {
					tenantId.set(requestedTenantId);
				}
				if (knowledgeId != null) {
					knowledgeId.set(requestedKnowledgeId);
				}
				return connections;
			}

			@Override
			public void save(DifyConnection connection) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void delete(String requestedTenantId, String connectionId) {
				throw new UnsupportedOperationException();
			}
		};
	}

	private TinyflowDifyConnectionRepository connectionRepository(TinyflowDifyConnectionRepository.DifyConnection connection,
			AtomicReference<String> tenantId, AtomicReference<String> knowledgeId) {
		return connectionRepository(Collections.singletonList(connection), tenantId, knowledgeId);
	}

	private TinyflowDifyConnectionRepository.DifyConnection connection() {
		return new TinyflowDifyConnectionRepository.DifyConnection("conn-1", "tenant-1", "knowledge-1",
				"https://dify.example", "dataset-7", "credential-1");
	}

	private TinyflowKnowledgeRequest request(String query, int topK, Double threshold) {
		return TinyflowKnowledgeRequest.builder().providerKey("dify").tenantId("tenant-1")
				.knowledgeId("knowledge-1").query(query).topK(topK).threshold(threshold).build();
	}

	private RetrieveResult retrieveResult() {
		Document document = new Document();
		document.setId("document-7");
		document.setName("notion page");
		document.setDataSourceType("notion");
		JSONObject dataSourceInfo = new JSONObject();
		dataSourceInfo.put("id", "external-source-7");
		document.setDataSourceInfo(dataSourceInfo);
		Segment segment = Segment.builder().id("segment-7").documentId("document-7")
				.content("retrieved content").position(3).tokens(17L).enabled(true).document(document).build();
		RetrieveResult.Record record = new RetrieveResult.Record();
		record.setScore(0.93D);
		record.setSegment(segment);
		RetrieveResult result = new RetrieveResult();
		result.setRecords(Collections.singletonList(record));
		return result;
	}

	private IDatasetClient datasetClient(final RetrieveResult result, final AtomicReference<String> datasetId,
			AtomicReference<String> query, final AtomicReference<RetrievalModel> retrievalModel, final RuntimeException failure) {
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] arguments) {
				if ("retrieve".equals(method.getName())) {
					if (datasetId != null) {
						datasetId.set((String) arguments[0]);
					}
					if (query != null) {
						query.set((String) arguments[1]);
					}
					if (retrievalModel != null) {
						retrievalModel.set((RetrievalModel) arguments[2]);
					}
					if (failure != null) {
						throw failure;
					}
					return result;
				}
				throw new UnsupportedOperationException(method.getName());
			}
		};
		return (IDatasetClient) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {IDatasetClient.class}, handler);
	}
}
