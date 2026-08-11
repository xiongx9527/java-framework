package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

public interface TinyflowKnowledgeAccessKeyRepository {

	AccessKey findByHash(String accessKeyHash);

	default AccessKey findByPrefix(String prefix) {
		return null;
	}

	void save(AccessKey accessKey);

	void delete(String tenantId, String knowledgeId, String accessKeyId);

	final class AccessKey {
		private static final String STRONG_ALGORITHM = "PBKDF2WithHmacSHA256";
		private static final int MINIMUM_ITERATIONS = 100000;

		private final String accessKeyId;
		private final String tenantId;
		private final String knowledgeId;
		private final String prefix;
		private final String hashAlgorithm;
		private final String salt;
		private final int cost;
		private final String accessKeyHash;
		private final boolean enabled;

		/**
		 * Legacy constructor retained for source and binary compatibility. Existing records use their
		 * access key id as the lookup prefix; newly issued keys use the explicit random prefix constructor.
		 */
		public AccessKey(String accessKeyId, String tenantId, String knowledgeId,
				String hashAlgorithm, String salt, int cost, String accessKeyHash, boolean enabled) {
			this(accessKeyId, tenantId, knowledgeId, accessKeyId, hashAlgorithm, salt, cost, accessKeyHash, enabled);
		}

		public AccessKey(String accessKeyId, String tenantId, String knowledgeId,
				String prefix, String hashAlgorithm, String salt, int cost, String accessKeyHash, boolean enabled) {
			this.accessKeyId = TinyflowKnowledgeReference.requireText(accessKeyId, "accessKeyId");
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.knowledgeId = TinyflowKnowledgeReference.requireText(knowledgeId, "knowledgeId");
			this.prefix = TinyflowKnowledgeReference.requireText(prefix, "prefix");
			this.hashAlgorithm = TinyflowKnowledgeReference.requireText(hashAlgorithm, "hashAlgorithm");
			if (!STRONG_ALGORITHM.equals(this.hashAlgorithm)) {
				throw new IllegalArgumentException("Unsupported or weak access key hash algorithm");
			}
			this.salt = TinyflowKnowledgeReference.requireText(salt, "salt");
			if (cost < MINIMUM_ITERATIONS) {
				throw new IllegalArgumentException("Access key hash cost must be at least " + MINIMUM_ITERATIONS);
			}
			this.cost = cost;
			this.accessKeyHash = TinyflowKnowledgeReference.requireText(accessKeyHash, "accessKeyHash");
			this.enabled = enabled;
		}

		public String getAccessKeyId() { return accessKeyId; }
		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getPrefix() { return prefix; }
		public String getHashAlgorithm() { return hashAlgorithm; }
		public String getSalt() { return salt; }
		public int getCost() { return cost; }
		public String getAccessKeyHash() { return accessKeyHash; }
		public boolean isEnabled() { return enabled; }
	}
}
