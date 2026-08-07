package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowCredentialResolver;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDifyConnectionRepository;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DefaultMagicTinyflowDifyConnectionTesterTest {

	@Test
	public void shouldResolveTenantCredentialAndProbeDataset() {
		AtomicReference<String> resolved = new AtomicReference<String>();
		AtomicReference<String> probed = new AtomicReference<String>();
		TinyflowCredentialResolver resolver = (tenantId, credentialRef) -> {
			resolved.set(tenantId + ":" + credentialRef);
			return new TinyflowCredentialResolver.ResolvedCredential("secret-key".toCharArray(), Long.MAX_VALUE);
		};
		DefaultMagicTinyflowDifyConnectionTester tester = new DefaultMagicTinyflowDifyConnectionTester(
				resolver, (baseUrl, apiKey, datasetId) ->
						probed.set(baseUrl + ":" + apiKey + ":" + datasetId), () -> 100L);

		tester.test(connection());

		assertEquals("tenant-a:credential-a", resolved.get());
		assertEquals("https://dify.example:secret-key:dataset-a", probed.get());
	}

	@Test
	public void shouldRejectExpiredCredentialBeforeProbingDify() {
		AtomicReference<Boolean> probed = new AtomicReference<Boolean>(false);
		TinyflowCredentialResolver resolver = (tenantId, credentialRef) ->
				new TinyflowCredentialResolver.ResolvedCredential("expired".toCharArray(), 99L);
		DefaultMagicTinyflowDifyConnectionTester tester = new DefaultMagicTinyflowDifyConnectionTester(
				resolver, (baseUrl, apiKey, datasetId) -> probed.set(true), () -> 100L);

		IllegalStateException error = assertThrows(IllegalStateException.class,
				() -> tester.test(connection()));

		assertEquals("Dify credential is expired", error.getMessage());
		assertFalse(probed.get());
		assertFalse(error.getMessage().contains("secret-key"));
	}

	@Test
	public void shouldRejectUnavailableCredential() {
		DefaultMagicTinyflowDifyConnectionTester tester = new DefaultMagicTinyflowDifyConnectionTester(
				(tenantId, credentialRef) -> null,
				(baseUrl, apiKey, datasetId) -> { throw new AssertionError("must not probe"); }, () -> 100L);

		IllegalStateException error = assertThrows(IllegalStateException.class,
				() -> tester.test(connection()));

		assertEquals("Dify credential is unavailable", error.getMessage());
		assertTrue(error.getMessage().indexOf("secret") < 0);
	}

	private TinyflowDifyConnectionRepository.DifyConnection connection() {
		return new TinyflowDifyConnectionRepository.DifyConnection("connection-a", "tenant-a", "knowledge-a",
				"https://dify.example", "dataset-a", "credential-a");
	}
}
