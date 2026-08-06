package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.List;
import java.util.Map;

public interface TinyflowDifyConnectionRepository {

	DifyConnection find(String tenantId, String connectionId);

	List<DifyConnection> findByKnowledge(String tenantId, String knowledgeId);

	void save(DifyConnection connection);

	void delete(String tenantId, String connectionId);

	final class DifyConnection {
		private final String connectionId;
		private final String tenantId;
		private final String knowledgeId;
		private final String baseUrl;
		private final String datasetId;
		private final String credentialRef;
		private final Map<String, Object> metadata;

		public DifyConnection(String connectionId, String tenantId, String knowledgeId, String baseUrl,
				String datasetId, String credentialRef, Map<String, Object> metadata) {
			this.connectionId = TinyflowKnowledgeReference.requireText(connectionId, "connectionId");
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.knowledgeId = TinyflowKnowledgeReference.requireText(knowledgeId, "knowledgeId");
			this.baseUrl = TinyflowKnowledgeReference.requireText(baseUrl, "baseUrl");
			this.datasetId = TinyflowKnowledgeReference.requireText(datasetId, "datasetId");
			this.credentialRef = TinyflowKnowledgeReference.requireText(credentialRef, "credentialRef");
			this.metadata = TinyflowKnowledgeRequest.immutableMap(metadata);
		}

		public String getConnectionId() { return connectionId; }
		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getBaseUrl() { return baseUrl; }
		public String getDatasetId() { return datasetId; }
		public String getCredentialRef() { return credentialRef; }
		public Map<String, Object> getMetadata() { return metadata; }
	}
}
