package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TinyEngineOfficialAiRequest {

	private static final String BEARER_PREFIX = "Bearer ";
	private final String baseUrl;
	private final String model;
	private final String apiKey;
	private final String authorization;
	private final Map<String, Object> forwardBody;

	private TinyEngineOfficialAiRequest(String baseUrl, String model, String apiKey,
			Map<String, Object> forwardBody) {
		this.baseUrl = baseUrl;
		this.model = model;
		this.apiKey = apiKey;
		this.authorization = apiKey == null ? null : BEARER_PREFIX + apiKey;
		this.forwardBody = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(forwardBody));
	}

	public static TinyEngineOfficialAiRequest parse(Map<String, Object> body, String authorizationHeader,
			TinyEngineAiCredentialCipher cipher) {
		Map<String, Object> source = body == null ? Collections.<String, Object>emptyMap() : body;
		Map<String, Object> forwardBody = new LinkedHashMap<String, Object>(source);
		forwardBody.remove("baseUrl");
		forwardBody.remove("apiKey");
		String apiKey = resolveApiKey(authorizationHeader, source.get("apiKey"), cipher);
		return new TinyEngineOfficialAiRequest(asString(source.get("baseUrl")), asString(source.get("model")),
				apiKey, forwardBody);
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getModel() {
		return model;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getAuthorization() {
		return authorization;
	}

	public Map<String, Object> getForwardBody() {
		return forwardBody;
	}

	private static String resolveApiKey(String authorizationHeader, Object bodyApiKey,
			TinyEngineAiCredentialCipher cipher) {
		String candidate = asString(bodyApiKey);
		if (candidate == null) {
			return null;
		}
		if (candidate.startsWith(Sm4TinyEngineAiCredentialCipher.TOKEN_PREFIX)) {
			if (cipher == null) {
				throw new IllegalStateException("TinyEngine AI credential cipher is required for EKEY_ token");
			}
			return cipher.decrypt(candidate);
		}
		return candidate;
	}

	private static String asString(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
