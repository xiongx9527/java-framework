package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowKnowledgeSecurityContractTest {

	@Test
	public void shouldRejectPlainBareShaAndLowCostAccessKeyHashes() {
		assertThrows(IllegalArgumentException.class,
				() -> accessKey("plain", "salt", 100000, "hash"));
		assertThrows(IllegalArgumentException.class,
				() -> accessKey("SHA-256", "salt", 100000, "hash"));
		assertThrows(IllegalArgumentException.class,
				() -> accessKey("PBKDF2WithHmacSHA256", "salt", 99999, "hash"));
		assertThrows(IllegalArgumentException.class,
				() -> accessKey("PBKDF2WithHmacSHA256", " ", 100000, "hash"));
	}

	@Test
	public void shouldKeepStrongHashParametersWithoutPlaintext() {
		TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey =
				accessKey("visible-prefix", "PBKDF2WithHmacSHA256", "random-salt", 120000, "derived-hash");
		assertEquals("visible-prefix", accessKey.getPrefix());
		assertEquals("PBKDF2WithHmacSHA256", accessKey.getHashAlgorithm());
		assertEquals("random-salt", accessKey.getSalt());
		assertEquals(120000, accessKey.getCost());
		assertEquals("derived-hash", accessKey.getAccessKeyHash());
	}

	@Test
	public void shouldKeepLegacyEightArgumentAccessKeyConstructorCompatible() {
		TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey =
				new TinyflowKnowledgeAccessKeyRepository.AccessKey("legacy-key", "tenant-a", "kb-1",
						"PBKDF2WithHmacSHA256", "random-salt", 120000, "derived-hash", true);

		assertEquals("legacy-key", accessKey.getPrefix());
		assertEquals("derived-hash", accessKey.getAccessKeyHash());
	}

	@Test
	public void shouldExposePrefixLookupAsDefaultRepositoryMethod() throws Exception {
		Method method = TinyflowKnowledgeAccessKeyRepository.class.getMethod("findByPrefix", String.class);

		assertTrue(method.isDefault());
		assertNull(new LegacyRepository().findByPrefix("unknown-prefix"));
	}

	@Test
	public void shouldExposeNoArbitraryMetadataEntryOnSensitiveRecords() {
		assertNoMapFieldOrConstructorParameter(TinyflowKnowledgeAccessKeyRepository.AccessKey.class);
		assertNoMapFieldOrConstructorParameter(TinyflowDifyConnectionRepository.DifyConnection.class);
		assertNoMapFieldOrConstructorParameter(TinyflowKnowledgeAuditSink.AuditEvent.class);
	}

	private TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey(String algorithm, String salt,
			int cost, String hash) {
		return accessKey("visible-prefix", algorithm, salt, cost, hash);
	}

	private TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey(String prefix, String algorithm, String salt,
			int cost, String hash) {
		return new TinyflowKnowledgeAccessKeyRepository.AccessKey(
				"key-1", "tenant-a", "kb-1", prefix, algorithm, salt, cost, hash, true);
	}

	private void assertNoMapFieldOrConstructorParameter(Class<?> type) {
		for (Field field : type.getDeclaredFields()) {
			assertFalse("Map field leaks arbitrary sensitive data: " + field.getName(),
					Map.class.isAssignableFrom(field.getType()));
		}
		for (Constructor<?> constructor : type.getDeclaredConstructors()) {
			for (Class<?> parameterType : constructor.getParameterTypes()) {
				assertFalse("Map constructor parameter leaks arbitrary sensitive data",
						Map.class.isAssignableFrom(parameterType));
			}
		}
	}

	private static final class LegacyRepository implements TinyflowKnowledgeAccessKeyRepository {
		@Override public AccessKey findByHash(String accessKeyHash) { return null; }
		@Override public void save(AccessKey accessKey) { }
		@Override public void delete(String tenantId, String knowledgeId, String accessKeyId) { }
	}
}
