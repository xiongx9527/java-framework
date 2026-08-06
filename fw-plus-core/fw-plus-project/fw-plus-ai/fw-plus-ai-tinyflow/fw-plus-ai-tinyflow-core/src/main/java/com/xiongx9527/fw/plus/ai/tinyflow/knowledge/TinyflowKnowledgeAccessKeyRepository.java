package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.Map;

public interface TinyflowKnowledgeAccessKeyRepository {

	AccessKey findByHash(String accessKeyHash);

	void save(AccessKey accessKey);

	void delete(String tenantId, String knowledgeId, String accessKeyId);

	final class AccessKey {
		private final String accessKeyId;
		private final String tenantId;
		private final String knowledgeId;
		private final String accessKeyHash;
		private final boolean enabled;
		private final Map<String, Object> metadata;

		public AccessKey(String accessKeyId, String tenantId, String knowledgeId,
				String accessKeyHash, boolean enabled, Map<String, Object> metadata) {
			this.accessKeyId = TinyflowKnowledgeReference.requireText(accessKeyId, "accessKeyId");
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.knowledgeId = TinyflowKnowledgeReference.requireText(knowledgeId, "knowledgeId");
			this.accessKeyHash = TinyflowKnowledgeReference.requireText(accessKeyHash, "accessKeyHash");
			this.enabled = enabled;
			this.metadata = TinyflowKnowledgeRequest.immutableMap(metadata);
		}

		public String getAccessKeyId() { return accessKeyId; }
		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getAccessKeyHash() { return accessKeyHash; }
		public boolean isEnabled() { return enabled; }
		public Map<String, Object> getMetadata() { return metadata; }
	}
}
