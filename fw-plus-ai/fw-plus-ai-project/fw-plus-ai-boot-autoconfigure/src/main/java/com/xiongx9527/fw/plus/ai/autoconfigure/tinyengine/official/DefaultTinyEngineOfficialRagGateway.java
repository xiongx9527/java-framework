package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagMatch;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultTinyEngineOfficialRagGateway implements TinyEngineOfficialRagGateway {

	private final TinyEngineRagService ragService;
	private final ObjectMapper objectMapper;

	public DefaultTinyEngineOfficialRagGateway(TinyEngineRagService ragService, ObjectMapper objectMapper) {
		this.ragService = ragService;
		this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
	}

	@Override
	public TinyEngineOfficialAiResponse search(Map<String, Object> request) {
		if (ragService == null || !ragService.isAvailable()) {
			return error(503, unavailableReason());
		}
		String content = asString(value(request, "content"));
		if (isBlank(content)) {
			return error(400, "TinyEngine AI RAG content is required");
		}
		TinyEngineRagRequest ragRequest = new TinyEngineRagRequest();
		ragRequest.setQuery(content);
		ragRequest.setCollection(firstText(request, "collection", "documentSetId"));
		ragRequest.setMaxResults(asInt(value(request, "maxResults")));
		ragRequest.setMinScore(asDouble(value(request, "minScore")));
		try {
			TinyEngineRagSearchResult result = ragService.search(ragRequest);
			return ok(toOfficialItems(result));
		} catch (RuntimeException ex) {
			return error(503, "TinyEngine AI RAG is unavailable");
		}
	}

	private List<Map<String, Object>> toOfficialItems(TinyEngineRagSearchResult result) {
		if (result == null || result.getMatches() == null || result.getMatches().isEmpty()) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		for (TinyEngineRagMatch match : result.getMatches()) {
			if (match == null) {
				continue;
			}
			Map<String, Object> item = new LinkedHashMap<String, Object>();
			item.put("score", match.getScore());
			item.put("content", match.getContent());
			item.put("doc_name", docName(match.getMetadata()));
			items.add(item);
		}
		return items;
	}

	private String docName(Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return null;
		}
		String docName = firstText(metadata, "doc_name", "documentName");
		return isBlank(docName) ? firstText(metadata, "source") : docName;
	}

	private TinyEngineOfficialAiResponse ok(Object data) {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("code", "0");
		body.put("success", true);
		body.put("data", data);
		return json(200, body);
	}

	private TinyEngineOfficialAiResponse error(int statusCode, String message) {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("code", String.valueOf(statusCode));
		body.put("success", false);
		body.put("message", message);
		return json(statusCode, body);
	}

	private TinyEngineOfficialAiResponse json(int statusCode, Map<String, Object> body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			return new TinyEngineOfficialAiResponse(statusCode, headers, objectMapper.writeValueAsBytes(body));
		} catch (JsonProcessingException ex) {
			return new TinyEngineOfficialAiResponse(statusCode, headers,
					("{\"code\":\"" + statusCode + "\",\"success\":false,\"message\":\"TinyEngine AI RAG failed\"}")
							.getBytes(StandardCharsets.UTF_8));
		}
	}

	private String unavailableReason() {
		String reason = ragService == null ? null : ragService.getUnavailableReason();
		return isBlank(reason) ? "TinyEngine AI RAG is unavailable" : "TinyEngine AI RAG is unavailable: " + reason;
	}

	private static Object value(Map<String, Object> request, String key) {
		return request == null ? null : request.get(key);
	}

	private static String firstText(Map<String, Object> values, String... keys) {
		if (values == null) {
			return null;
		}
		for (String key : keys) {
			String value = asString(values.get(key));
			if (!isBlank(value)) {
				return value;
			}
		}
		return null;
	}

	private static String asString(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static int asInt(Object value) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		String text = asString(value);
		if (isBlank(text)) {
			return 0;
		}
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	private static double asDouble(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		String text = asString(value);
		if (isBlank(text)) {
			return 0.0D;
		}
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException ex) {
			return 0.0D;
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
