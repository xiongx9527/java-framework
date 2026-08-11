package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TinyflowKnowledgeAccessKeyServiceTest {

	@Test
	public void shouldRejectUnknownPrefixBeforeExpensiveHashPath() {
		CountingRepository repository = new CountingRepository();
		TinyflowKnowledgeAccessKeyService service = new TinyflowKnowledgeAccessKeyService(repository);

		assertThrows(TinyflowKnowledgeAccessKeyService.AuthenticationException.class,
				() -> service.authenticate(token(1)));

		assertEquals(1, repository.prefixLookups);
		assertEquals(0, repository.hashLookups);
	}

	@Test
	public void shouldBoundRepeatedUnknownPrefixBeforeRepositoryLookup() {
		CountingRepository repository = new CountingRepository();
		TinyflowKnowledgeAccessKeyService service = service(repository, 10, 2);

		for (int attempt = 0; attempt < 8; attempt++) {
			assertAuthenticationFailure(service, token(1));
		}

		assertEquals(2, repository.prefixLookups);
		assertEquals(0, repository.hashLookups);
	}

	@Test
	public void shouldBoundGlobalUnknownPrefixFloodBeforeRepositoryLookup() {
		CountingRepository repository = new CountingRepository();
		TinyflowKnowledgeAccessKeyService service = service(repository, 4, 2);

		for (int attempt = 0; attempt < 10; attempt++) {
			assertAuthenticationFailure(service, token(attempt + 1));
		}

		assertEquals(4, repository.prefixLookups);
		assertEquals(0, repository.hashLookups);
	}

	private TinyflowKnowledgeAccessKeyService service(CountingRepository repository,
			int globalLimit, int prefixLimit) {
		return new TinyflowKnowledgeAccessKeyService(repository, new SecureRandom(), globalLimit, prefixLimit,
				new LongSupplier() {
					@Override public long getAsLong() { return 0L; }
				});
	}

	private void assertAuthenticationFailure(TinyflowKnowledgeAccessKeyService service, String value) {
		assertThrows(TinyflowKnowledgeAccessKeyService.AuthenticationException.class,
				() -> service.authenticate(value));
	}

	private String token(int prefixSeed) {
		byte[] prefix = new byte[9];
		prefix[5] = (byte) (prefixSeed >>> 24);
		prefix[6] = (byte) (prefixSeed >>> 16);
		prefix[7] = (byte) (prefixSeed >>> 8);
		prefix[8] = (byte) prefixSeed;
		String prefixPart = Base64.getUrlEncoder().withoutPadding().encodeToString(prefix);
		String saltPart = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
		String secretPart = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
		return "tfk1." + prefixPart + "." + saltPart + "." + secretPart;
	}

	private static final class CountingRepository implements TinyflowKnowledgeAccessKeyRepository {
		private int prefixLookups;
		private int hashLookups;

		@Override public AccessKey findByPrefix(String prefix) {
			prefixLookups++;
			return null;
		}

		@Override public AccessKey findByHash(String accessKeyHash) {
			hashLookups++;
			return null;
		}

		@Override public void save(AccessKey accessKey) { }
		@Override public void delete(String tenantId, String knowledgeId, String accessKeyId) { }
	}

}
