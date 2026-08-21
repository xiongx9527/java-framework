package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowCredentialResolver;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDifyConnectionRepository;
import com.xiongx9527.fw.plus.dify.DifyClientBuilder;

import java.util.Arrays;
import java.util.function.LongSupplier;

public final class DefaultMagicTinyflowDifyConnectionTester
		implements MagicTinyflowKnowledgeController.DifyConnectionTester {

	private final TinyflowCredentialResolver credentialResolver;
	private final DatasetProbe datasetProbe;
	private final LongSupplier clock;

	public DefaultMagicTinyflowDifyConnectionTester(TinyflowCredentialResolver credentialResolver) {
		this(credentialResolver, new DatasetProbe() {
			@Override
			public void get(String baseUrl, String apiKey, String datasetId) {
				DifyClientBuilder.dataset(baseUrl, apiKey).build().get(datasetId);
			}
		}, System::currentTimeMillis);
	}

	DefaultMagicTinyflowDifyConnectionTester(TinyflowCredentialResolver credentialResolver,
			DatasetProbe datasetProbe, LongSupplier clock) {
		if (credentialResolver == null || datasetProbe == null || clock == null) {
			throw new IllegalArgumentException("Dify tester dependencies must not be null");
		}
		this.credentialResolver = credentialResolver;
		this.datasetProbe = datasetProbe;
		this.clock = clock;
	}

	@Override
	public void test(TinyflowDifyConnectionRepository.DifyConnection connection) {
		if (connection == null) {
			throw new IllegalArgumentException("connection must not be null");
		}
		char[] apiKey = null;
		try {
			TinyflowCredentialResolver.ResolvedCredential credential = credentialResolver.resolve(
					connection.getTenantId(), connection.getCredentialRef());
			if (credential == null) {
				throw new IllegalStateException("Dify credential is unavailable");
			}
			if (credential.getExpiresAtEpochMillis() <= clock.getAsLong()) {
				throw new IllegalStateException("Dify credential is expired");
			}
			apiKey = credential.getValue();
			datasetProbe.get(connection.getBaseUrl(), new String(apiKey), connection.getDatasetId());
		} finally {
			if (apiKey != null) {
				Arrays.fill(apiKey, '\0');
			}
		}
	}

	interface DatasetProbe {
		void get(String baseUrl, String apiKey, String datasetId);
	}
}
