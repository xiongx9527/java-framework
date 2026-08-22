package com.xiongx9527.fw.plus.ai.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowCredentialResolver;
import com.xiongx9527.fw.plus.dify.DifyClientBuilder;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;

import java.util.Arrays;

public final class DifyDatasetClientFactory {

	private final TinyflowCredentialResolver credentialResolver;
	private final DatasetClientCreator clientCreator;

	public DifyDatasetClientFactory(TinyflowCredentialResolver credentialResolver) {
		this(credentialResolver, new DatasetClientCreator() {
			@Override
			public IDatasetClient create(String baseUrl, String apiKey) {
				return DifyClientBuilder.dataset(baseUrl, apiKey).build();
			}
		});
	}

	DifyDatasetClientFactory(TinyflowCredentialResolver credentialResolver, DatasetClientCreator clientCreator) {
		if (credentialResolver == null || clientCreator == null) {
			throw new IllegalArgumentException("credentialResolver and clientCreator must not be null");
		}
		this.credentialResolver = credentialResolver;
		this.clientCreator = clientCreator;
	}

	public IDatasetClient create(DifyTinyflowConnection connection) {
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
			apiKey = credential.getValue();
			return clientCreator.create(connection.getBaseUrl(), new String(apiKey));
		} finally {
			if (apiKey != null) {
				Arrays.fill(apiKey, '\0');
			}
		}
	}

	interface DatasetClientCreator {
		IDatasetClient create(String baseUrl, String apiKey);
	}
}
