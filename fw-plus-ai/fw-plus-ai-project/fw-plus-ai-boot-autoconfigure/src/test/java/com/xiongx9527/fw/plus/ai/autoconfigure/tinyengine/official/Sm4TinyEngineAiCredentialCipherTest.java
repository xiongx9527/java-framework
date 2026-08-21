package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Sm4TinyEngineAiCredentialCipherTest {

	private static final String BASE64_SM4_KEY = "ASNFZ4mrze/+3LqYdlQyEA==";
	private static final String OFFICIAL_SECRET_TOKEN = "EKEY_kNc1RPJK5vHMRSnd+Zc1NQ==";

	@Test
	public void shouldEncryptAndDecryptOfficialTokenFormat() {
		TinyEngineAiCredentialCipher cipher = new Sm4TinyEngineAiCredentialCipher(BASE64_SM4_KEY);

		String token = cipher.encrypt("secret-value");

		assertTrue(token.startsWith("EKEY_"));
		assertEquals("secret-value", cipher.decrypt(token));
	}

	@Test
	public void shouldDecryptTokenGeneratedByOfficialSm4Utils() {
		TinyEngineAiCredentialCipher cipher = new Sm4TinyEngineAiCredentialCipher(BASE64_SM4_KEY);

		assertEquals("secret-value", cipher.decrypt(OFFICIAL_SECRET_TOKEN));
	}

	@Test
	public void shouldRejectMissingInvalidLengthAndInvalidTokensWithClearExceptions() {
		IllegalStateException missingKey = assertThrows(IllegalStateException.class,
				() -> new Sm4TinyEngineAiCredentialCipher(null).encrypt("secret-value"));
		assertTrue(missingKey.getMessage().contains("SM4 key"));

		IllegalArgumentException invalidKey = assertThrows(IllegalArgumentException.class,
				() -> new Sm4TinyEngineAiCredentialCipher("c2hvcnQ=").encrypt("secret-value"));
		assertTrue(invalidKey.getMessage().contains("16 bytes"));

		TinyEngineAiCredentialCipher cipher = new Sm4TinyEngineAiCredentialCipher(BASE64_SM4_KEY);
		IllegalArgumentException invalidToken = assertThrows(IllegalArgumentException.class,
				() -> cipher.decrypt("not-an-encrypted-token"));
		assertTrue(invalidToken.getMessage().contains("EKEY_"));
	}

	@Test
	public void shouldResolveExplicitSm4KeyBeforeEnvironment() {
		TinyEngineAiProperties properties = new TinyEngineAiProperties();
		properties.getOfficial().setSm4Key(BASE64_SM4_KEY);
		TinyEngineAiCredentialCipher cipher = Sm4TinyEngineAiCredentialCipher.from(properties.getOfficial());

		assertEquals("secret-value", cipher.decrypt(OFFICIAL_SECRET_TOKEN));
	}

	@Test
	public void shouldParseOfficialAiRequestAndPrepareForwardBody() {
		TinyEngineAiCredentialCipher cipher = new Sm4TinyEngineAiCredentialCipher(BASE64_SM4_KEY);
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("baseUrl", "https://example.test/v1");
		body.put("apiKey", OFFICIAL_SECRET_TOKEN);
		body.put("model", "gpt-test");
		body.put("temperature", 0.7);

		TinyEngineOfficialAiRequest request = TinyEngineOfficialAiRequest.parse(body, null, cipher);

		assertEquals("https://example.test/v1", request.getBaseUrl());
		assertEquals("gpt-test", request.getModel());
		assertEquals("secret-value", request.getApiKey());
		assertEquals("Bearer secret-value", request.getAuthorization());
		assertFalse(request.getForwardBody().containsKey("baseUrl"));
		assertFalse(request.getForwardBody().containsKey("apiKey"));
		assertEquals("gpt-test", request.getForwardBody().get("model"));
		assertEquals(0.7, request.getForwardBody().get("temperature"));
	}

	@Test
	public void shouldIgnoreEngineAuthorizationAndUseBodyApiKey() {
		TinyEngineAiCredentialCipher cipher = new Sm4TinyEngineAiCredentialCipher(BASE64_SM4_KEY);
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("apiKey", "body-key");
		body.put("model", "gpt-test");

		TinyEngineOfficialAiRequest request = TinyEngineOfficialAiRequest.parse(body, "Bearer header-key", cipher);

		assertEquals("body-key", request.getApiKey());
		assertEquals("Bearer body-key", request.getAuthorization());
		assertEquals("gpt-test", request.getForwardBody().get("model"));
		assertFalse(request.getForwardBody().containsKey("apiKey"));
	}
}
