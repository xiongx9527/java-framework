package com.xiongx9527.fw.plus.ai.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDifyConnectionRepository;

final class DifyTinyflowConnection {

	private final String tenantId;
	private final String knowledgeId;
	private final String baseUrl;
	private final String datasetId;
	private final String credentialRef;

	private DifyTinyflowConnection(TinyflowDifyConnectionRepository.DifyConnection connection) {
		this.tenantId = connection.getTenantId();
		this.knowledgeId = connection.getKnowledgeId();
		this.baseUrl = connection.getBaseUrl();
		this.datasetId = connection.getDatasetId();
		this.credentialRef = connection.getCredentialRef();
	}

	static DifyTinyflowConnection from(TinyflowDifyConnectionRepository.DifyConnection connection) {
		if (connection == null) {
			throw new IllegalArgumentException("connection must not be null");
		}
		return new DifyTinyflowConnection(connection);
	}

	String getTenantId() { return tenantId; }
	String getKnowledgeId() { return knowledgeId; }
	String getBaseUrl() { return baseUrl; }
	String getDatasetId() { return datasetId; }
	String getCredentialRef() { return credentialRef; }
}
