package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface TinyflowObjectStorage {

	StoredObject put(String tenantId, String objectKey, InputStream input, String mediaType,
			Map<String, Object> metadata) throws IOException;

	InputStream get(String tenantId, String objectKey) throws IOException;

	boolean exists(String tenantId, String objectKey);

	void delete(String tenantId, String objectKey);

	final class StoredObject {
		private final String tenantId;
		private final String objectKey;
		private final String mediaType;
		private final long size;
		private final String checksum;
		private final Map<String, Object> metadata;

		public StoredObject(String tenantId, String objectKey, String mediaType, long size,
				String checksum, Map<String, Object> metadata) {
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.objectKey = TinyflowKnowledgeReference.requireText(objectKey, "objectKey");
			this.mediaType = mediaType;
			if (size < 0L) {
				throw new IllegalArgumentException("size must not be negative");
			}
			this.size = size;
			this.checksum = checksum;
			this.metadata = TinyflowKnowledgeRequest.immutableMap(metadata);
		}

		public String getTenantId() { return tenantId; }
		public String getObjectKey() { return objectKey; }
		public String getMediaType() { return mediaType; }
		public long getSize() { return size; }
		public String getChecksum() { return checksum; }
		public Map<String, Object> getMetadata() { return metadata; }
	}
}
