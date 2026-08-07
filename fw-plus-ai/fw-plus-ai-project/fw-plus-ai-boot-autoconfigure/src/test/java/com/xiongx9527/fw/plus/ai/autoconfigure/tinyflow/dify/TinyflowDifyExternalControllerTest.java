package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeCapabilities;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TinyflowDifyExternalControllerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private InMemoryAccessKeyRepository accessKeys;
	private TinyflowKnowledgeRepository knowledgeRepository;
	private RecordingProvider provider;
	private TinyflowDifyExternalProperties properties;
	private TinyflowKnowledgeAccessKeyService accessKeyService;
	private TinyflowDifyExternalKnowledgeService service;
	private MockMvc mockMvc;
	private String token;

	@Before
	public void setUp() {
		accessKeys = new InMemoryAccessKeyRepository();
		knowledgeRepository = mock(TinyflowKnowledgeRepository.class);
		provider = new RecordingProvider();
		properties = new TinyflowDifyExternalProperties();
		properties.setEnabled(true);
		accessKeyService = new TinyflowKnowledgeAccessKeyService(accessKeys);
		token = accessKeyService.create("tenant-a", "kb-1").getToken();
		when(knowledgeRepository.findKnowledge("tenant-a", "kb-1")).thenReturn(definition("fw-plus-local", true));
		rebuildService();
	}

	@After
	public void tearDown() {
		if (service != null) {
			service.destroy();
		}
	}

	@Test
	public void shouldAuthenticateAndMapDifyResponseWithRequiredMetadata() throws Exception {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("custom", "value");
		provider.result = TinyflowKnowledgeResult.builder().matches(Collections.singletonList(
				TinyflowKnowledgeMatch.builder().tenantId("tenant-a").knowledgeId("kb-1")
						.sourceId("source-1").sourceType("file").documentId("doc-2").chunkId("chunk-9")
						.content("matched text").metadata(metadata).score(0.91D).build())).build();

		mockMvc.perform(post("/external-knowledge/retrieval")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.records[0].content").value("matched text"))
				.andExpect(jsonPath("$.records[0].metadata.source_id").value("source-1"))
				.andExpect(jsonPath("$.records[0].metadata.source_type").value("file"))
				.andExpect(jsonPath("$.records[0].metadata.document_id").value("doc-2"))
				.andExpect(jsonPath("$.records[0].metadata.chunk_id").value("chunk-9"));

		assertEquals("fw-plus-local", provider.request.getProviderKey());
		assertEquals("tenant-a", provider.request.getTenantId());
		assertEquals(3, provider.request.getTopK());
		assertEquals(Double.valueOf(0.2D), provider.request.getThreshold());

		com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeArgs args =
				objectMapper.readValue(validRequest(),
						com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeArgs.class);
		com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeResult mapped = service.retrieval(token, args);
		Map<String, Object> mappedMetadata = mapped.getRecords().get(0).getMetadata();
		assertTrue(mappedMetadata instanceof LinkedHashMap);
		assertEquals(Arrays.asList("custom", "source_id", "source_type", "document_id", "chunk_id"),
				new ArrayList<String>(mappedMetadata.keySet()));
		assertEquals("source-1", mappedMetadata.get("source_id"));
		assertEquals("file", mappedMetadata.get("source_type"));
		assertEquals("doc-2", mappedMetadata.get("document_id"));
		assertEquals("chunk-9", mappedMetadata.get("chunk_id"));
	}

	@Test
	public void shouldCreateStrongVersionedAccessKeyAndReturnPlaintextOnlyOnce() {
		TinyflowKnowledgeAccessKeyService.CreatedAccessKey created = accessKeyService.create("tenant-a", "kb-2");
		TinyflowKnowledgeAccessKeyRepository.AccessKey stored = accessKeys.lastSaved;
		assertTrue(created.getToken().startsWith("tfk1." + stored.getPrefix() + "."));
		assertNotEquals(stored.getAccessKeyId(), stored.getPrefix());
		assertEquals("PBKDF2WithHmacSHA256", stored.getHashAlgorithm());
		assertTrue(stored.getCost() >= 100000);
		assertNotEquals(created.getToken(), stored.getAccessKeyHash());
		assertFalse(stored.getAccessKeyHash().contains(created.getToken()));
		assertEquals(stored.getAccessKeyId(), accessKeyService.authenticate(created.getToken()).getAccessKeyId());
	}

	@Test
	public void shouldReturnStableAuthorizationErrorsWithoutIdentityDisclosure() throws Exception {
		assertError(null, 403, 1001);
		assertError("Basic value", 403, 1001);
		assertError("Bearer invalid", 401, 1002);

		TinyflowKnowledgeAccessKeyRepository.AccessKey saved = accessKeys.lastSaved;
		accessKeys.save(new TinyflowKnowledgeAccessKeyRepository.AccessKey(saved.getAccessKeyId(),
				saved.getTenantId(), saved.getKnowledgeId(), saved.getPrefix(), saved.getHashAlgorithm(),
				saved.getSalt(), saved.getCost(), saved.getAccessKeyHash(), false));
		assertError("Bearer " + token, 401, 1002);

		String scopedElsewhere = accessKeyService.create("tenant-a", "kb-other").getToken();
		assertError("Bearer " + scopedElsewhere, 401, 1002);
		Map<String, Object> invalidBody = requestMap();
		@SuppressWarnings("unchecked")
		Map<String, Object> invalidSetting = (Map<String, Object>) invalidBody.get("retrieval_setting");
		invalidSetting.put("top_k", Integer.valueOf(21));
		mockMvc.perform(post("/external-knowledge/retrieval")
				.header("Authorization", "Bearer " + scopedElsewhere)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidBody)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error_code").value(1002));
	}

	@Test
	public void shouldValidateTopKThresholdAndMalformedJsonBeforeProviderCall() throws Exception {
		assertBodyError("{\"knowledge_id\":\"kb-1\",\"query\":\"q\",\"retrieval_setting\":{\"top_k\":21}}", 400, 1003);
		assertBodyError("{\"knowledge_id\":\"kb-1\",\"query\":\"q\",\"retrieval_setting\":{\"top_k\":1.5}}", 400, 1003);
		assertBodyError("{\"knowledge_id\":\"kb-1\",\"query\":\"q\",\"retrieval_setting\":{\"top_k\":3,\"score_threshold\":1.1}}", 400, 1003);
		assertEquals(0, provider.calls);
	}

	@Test
	public void shouldReturnStableJsonErrorForBrokenJson() throws Exception {
		mockMvc.perform(post("/external-knowledge/retrieval").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"knowledge_id\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error_code").value(1003))
				.andExpect(jsonPath("$.error_msg").isString());
		assertEquals(0, provider.calls);
	}

	@Test
	public void shouldRejectMalformedAndTamperedTokensWithTheSameError() throws Exception {
		assertError("Bearer tfk2.prefix.salt.secret", 401, 1002);
		assertError("Bearer tfk1.prefix.salt", 401, 1002);
		String replacement = token.endsWith("A") ? "B" : "A";
		assertError("Bearer " + token.substring(0, token.length() - 1) + replacement, 401, 1002);
		assertEquals(0, provider.calls);
	}

	@Test
	public void shouldMapAndOrAndEverySupportedMetadataOperator() throws Exception {
		List<String> operators = Arrays.asList("contains", "not contains", "start with", "end with",
				"is", "is not", "in", "not in", "empty", "not empty", "=", "\u2260", ">", "<", "\u2265", "\u2264",
				"before", "after");
		Map<String, Object> request = requestMap();
		Map<String, Object> metadataCondition = new LinkedHashMap<String, Object>();
		metadataCondition.put("logical_operator", "or");
		List<Map<String, Object>> conditions = new ArrayList<Map<String, Object>>();
		for (String operator : operators) {
			Map<String, Object> condition = new LinkedHashMap<String, Object>();
			condition.put("name", "field-" + conditions.size());
			condition.put("comparison_operator", operator);
			condition.put("value", "expected");
			conditions.add(condition);
		}
		metadataCondition.put("conditions", conditions);
		request.put("metadata_condition", metadataCondition);

		mockMvc.perform(post("/external-knowledge/retrieval").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
		assertEquals("or", provider.request.getRetrievalOptions().get("metadataLogic"));
		assertEquals(operators.size(), provider.request.getMetadataConditions().size());
		for (int index = 0; index < operators.size(); index++) {
			assertEquals(operators.get(index), provider.request.getMetadataConditions().get(index).getOperator());
		}

		metadataCondition.put("logical_operator", "and");
		mockMvc.perform(post("/external-knowledge/retrieval").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
		assertEquals("and", provider.request.getRetrievalOptions().get("metadataLogic"));
	}

	@Test
	public void shouldReturnEmptyRecordsForZeroResults() throws Exception {
		mockMvc.perform(post("/external-knowledge/retrieval").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(validRequest()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.records").isEmpty());
	}

	@Test
	public void shouldRejectMissingKnowledgeAndDifyLoopBeforeProviderCall() throws Exception {
		when(knowledgeRepository.findKnowledge("tenant-a", "kb-1")).thenReturn(null);
		assertBodyError(validRequest(), 404, 2001);
		when(knowledgeRepository.findKnowledge("tenant-a", "kb-1")).thenReturn(definition("dify", true));
		assertBodyError(validRequest(), 409, 2002);
		assertEquals(0, provider.calls);
	}

	@Test
	public void shouldRejectProviderWithoutRetrievalCapability() throws Exception {
		provider.capabilities = TinyflowKnowledgeCapabilities.of(false, true, true);
		assertBodyError(validRequest(), 503, 2003);
		assertEquals(0, provider.calls);
	}

	@Test
	public void shouldRateLimitByAuthenticatedKeyAtConfiguredBoundary() throws Exception {
		properties.setRateLimit(2);
		rebuildService();
		for (int index = 0; index < 2; index++) {
			mockMvc.perform(post("/external-knowledge/retrieval").header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON).content(validRequest()))
					.andExpect(status().isOk());
		}
		assertBodyError(validRequest(), 429, 3001);

		String secondKey = accessKeyService.create("tenant-a", "kb-1").getToken();
		mockMvc.perform(post("/external-knowledge/retrieval").header("Authorization", "Bearer " + secondKey)
				.contentType(MediaType.APPLICATION_JSON).content(validRequest()))
				.andExpect(status().isOk());
	}

	@Test
	public void shouldConvertTimeoutAndInternalFailureToStableErrors() throws Exception {
		properties.setRequestTimeout(Duration.ofMillis(25));
		provider.delayMillis = 250L;
		rebuildService();
		assertBodyError(validRequest(), 504, 3002);

		provider.delayMillis = 0L;
		provider.failure = new IllegalStateException("sensitive cause");
		assertBodyError(validRequest(), 500, 5000);
		mockMvc.perform(post("/external-knowledge/retrieval").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(validRequest()))
				.andExpect(status().isInternalServerError())
				.andExpect(result -> assertFalse(result.getResponse().getContentAsString()
						.contains("sensitive cause")));
	}

	private void assertError(String authorization, int statusCode, int errorCode) throws Exception {
		org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
				post("/external-knowledge/retrieval").contentType(MediaType.APPLICATION_JSON).content(validRequest());
		if (authorization != null) {
			request.header("Authorization", authorization);
		}
		mockMvc.perform(request).andExpect(status().is(statusCode))
				.andExpect(jsonPath("$.error_code").value(errorCode))
				.andExpect(jsonPath("$.error_msg").isString());
	}

	private void assertBodyError(String body, int statusCode, int errorCode) throws Exception {
		mockMvc.perform(post("/external-knowledge/retrieval").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().is(statusCode))
				.andExpect(jsonPath("$.error_code").value(errorCode))
				.andExpect(jsonPath("$.error_msg").isString());
	}

	private void rebuildService() {
		if (service != null) {
			service.destroy();
		}
		service = new TinyflowDifyExternalKnowledgeService(
				new TinyflowKnowledgeProviderRegistry(Collections.<TinyflowKnowledgeProvider>singletonList(provider)),
				knowledgeRepository, accessKeyService, properties);
		mockMvc = MockMvcBuilders.standaloneSetup(new TinyflowDifyExternalController(service)).build();
	}

	private String validRequest() throws Exception {
		return objectMapper.writeValueAsString(requestMap());
	}

	private Map<String, Object> requestMap() {
		Map<String, Object> setting = new LinkedHashMap<String, Object>();
		setting.put("top_k", Integer.valueOf(3));
		setting.put("score_threshold", Double.valueOf(0.2D));
		Map<String, Object> request = new LinkedHashMap<String, Object>();
		request.put("knowledge_id", "kb-1");
		request.put("query", "question");
		request.put("retrieval_setting", setting);
		return request;
	}

	private TinyflowKnowledgeDefinition definition(String providerKey, boolean enabled) {
		return TinyflowKnowledgeDefinition.builder().providerKey(providerKey).tenantId("tenant-a")
				.knowledgeId("kb-1").name("Knowledge").enabled(enabled).build();
	}

	private static final class RecordingProvider implements TinyflowKnowledgeProvider {
		private TinyflowKnowledgeCapabilities capabilities = TinyflowKnowledgeCapabilities.retrievalOnly();
		private TinyflowKnowledgeResult result = TinyflowKnowledgeResult.empty();
		private TinyflowKnowledgeRequest request;
		private RuntimeException failure;
		private long delayMillis;
		private int calls;

		@Override public String getKey() { return "fw-plus-local"; }
		@Override public TinyflowKnowledgeCapabilities capabilities() { return capabilities; }
		@Override public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest value) {
			calls++;
			request = value;
			if (delayMillis > 0L) {
				try {
					Thread.sleep(delayMillis);
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
				}
			}
			if (failure != null) {
				throw failure;
			}
			return result;
		}
	}

	private static final class InMemoryAccessKeyRepository implements TinyflowKnowledgeAccessKeyRepository {
		private final Map<String, AccessKey> values = new ConcurrentHashMap<String, AccessKey>();
		private AccessKey lastSaved;
		@Override public AccessKey findByHash(String accessKeyHash) { return values.get(accessKeyHash); }
		@Override public void save(AccessKey accessKey) {
			lastSaved = accessKey;
			values.put(accessKey.getAccessKeyHash(), accessKey);
		}
		@Override public void delete(String tenantId, String knowledgeId, String accessKeyId) {
			for (AccessKey key : new ArrayList<AccessKey>(values.values())) {
				if (tenantId.equals(key.getTenantId()) && knowledgeId.equals(key.getKnowledgeId())
						&& accessKeyId.equals(key.getAccessKeyId())) {
					values.remove(key.getAccessKeyHash());
				}
			}
		}
	}
}
