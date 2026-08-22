package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagMatch;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TinyEngineOfficialRagGatewayTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void shouldMapOfficialSearchRequestAndReturnOfficialResultItems() throws Exception {
		RecordingRagService service = new RecordingRagService(true, null);
		DefaultTinyEngineOfficialRagGateway gateway = new DefaultTinyEngineOfficialRagGateway(service, objectMapper);
		Map<String, Object> request = new LinkedHashMap<String, Object>();
		request.put("content", "button component");
		request.put("documentSetId", "docs-a");
		request.put("maxResults", 3);
		request.put("minScore", 0.78D);

		TinyEngineOfficialAiResponse response = gateway.search(request);

		assertEquals(200, response.getStatusCode());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
		assertEquals("button component", service.lastRequest.getQuery());
		assertEquals("docs-a", service.lastRequest.getCollection());
		assertEquals(3, service.lastRequest.getMaxResults());
		assertEquals(0.78D, service.lastRequest.getMinScore(), 0.0001D);
		JsonNode body = objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
		assertEquals("0", body.path("code").asText());
		assertTrue(body.path("success").asBoolean());
		assertEquals(0.91D, body.path("data").get(0).path("score").asDouble(), 0.0001D);
		assertEquals("content-a", body.path("data").get(0).path("content").asText());
		assertEquals("official-doc", body.path("data").get(0).path("doc_name").asText());
		assertEquals("document-name", body.path("data").get(1).path("doc_name").asText());
		assertFalse(body.path("data").get(0).has("metadata"));
		assertFalse(body.has("matches"));
	}

	@Test
	public void shouldReturnUnavailableErrorWhenRagServiceIsUnavailable() throws Exception {
		RecordingRagService service = new RecordingRagService(false, "embedding store missing");
		DefaultTinyEngineOfficialRagGateway gateway = new DefaultTinyEngineOfficialRagGateway(service, objectMapper);

		TinyEngineOfficialAiResponse response = gateway.search(Collections.<String, Object>singletonMap("content", "x"));

		assertEquals(503, response.getStatusCode());
		JsonNode body = objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
		assertEquals("503", body.path("code").asText());
		assertFalse(body.path("success").asBoolean());
		assertTrue(body.path("message").asText().contains("embedding store missing"));
	}

	private static final class RecordingRagService implements TinyEngineRagService {
		private final boolean available;
		private final String unavailableReason;
		private TinyEngineRagRequest lastRequest;

		private RecordingRagService(boolean available, String unavailableReason) {
			this.available = available;
			this.unavailableReason = unavailableReason;
		}

		@Override
		public boolean isAvailable() {
			return available;
		}

		@Override
		public String getUnavailableReason() {
			return unavailableReason;
		}

		@Override
		public TinyEngineRagSearchResult search(TinyEngineRagRequest request) {
			this.lastRequest = request;
			Map<String, Object> metadataA = new LinkedHashMap<String, Object>();
			metadataA.put("doc_name", "official-doc");
			Map<String, Object> metadataB = new LinkedHashMap<String, Object>();
			metadataB.put("documentName", "document-name");
			return new TinyEngineRagSearchResult(Arrays.asList(
					new TinyEngineRagMatch(0.91D, "content-a", metadataA),
					new TinyEngineRagMatch(0.82D, "content-b", metadataB)));
		}
	}
}
