package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persisted Tinyflow knowledge record. Secrets and request credentials must never be added here. */
public class TinyflowKnowledgeMagicResource extends MagicEntity {

	private String recordType;
	private String tenantId;
	private String knowledgeId;
	private String providerKey;
	private String displayName;
	private String description;
	private Map<String, Object> metadata;
	private boolean enabled = true;
	private long version;
	private String vectorStoreKey;
	private Boolean metadataFilterSupported;
	private String sourceId;
	private String sourceType;
	private String documentId;
	private String checksum;
	private String content;
	private String sourceUri;
	private String sourceVersion;
	private boolean active;
	private String chunkId;
	private String location;
	private Integer position;
	private String parentChunkId;
	private Integer tokenCount;
	private List<String> vectorIds;
	private String taskId;
	private String status;
	private String message;
	private String compensationId;
	private String operation;
	private String stage;
	private String objectKey;
	private String accessKeyId;
	private String prefix;
	private String hashAlgorithm;
	private String salt;
	private int cost;
	private String accessKeyHash;
	private String connectionId;
	private String baseUrl;
	private String datasetId;
	private String credentialRef;

	public TinyflowKnowledgeMagicResource copyResource() {
		TinyflowKnowledgeMagicResource copy = new TinyflowKnowledgeMagicResource();
		copyTo(copy);
		copy.recordType = recordType;
		copy.tenantId = tenantId;
		copy.knowledgeId = knowledgeId;
		copy.providerKey = providerKey;
		copy.displayName = displayName;
		copy.description = description;
		copy.metadata = metadata == null ? null : new LinkedHashMap<String, Object>(metadata);
		copy.enabled = enabled;
		copy.version = version;
		copy.vectorStoreKey = vectorStoreKey;
		copy.metadataFilterSupported = metadataFilterSupported;
		copy.sourceId = sourceId;
		copy.sourceType = sourceType;
		copy.documentId = documentId;
		copy.checksum = checksum;
		copy.content = content;
		copy.sourceUri = sourceUri;
		copy.sourceVersion = sourceVersion;
		copy.active = active;
		copy.chunkId = chunkId;
		copy.location = location;
		copy.position = position;
		copy.parentChunkId = parentChunkId;
		copy.tokenCount = tokenCount;
		copy.vectorIds = vectorIds == null ? null : new ArrayList<String>(vectorIds);
		copy.taskId = taskId;
		copy.status = status;
		copy.message = message;
		copy.compensationId = compensationId;
		copy.operation = operation;
		copy.stage = stage;
		copy.objectKey = objectKey;
		copy.accessKeyId = accessKeyId;
		copy.prefix = prefix;
		copy.hashAlgorithm = hashAlgorithm;
		copy.salt = salt;
		copy.cost = cost;
		copy.accessKeyHash = accessKeyHash;
		copy.connectionId = connectionId;
		copy.baseUrl = baseUrl;
		copy.datasetId = datasetId;
		copy.credentialRef = credentialRef;
		return copy;
	}

	@Override
	public MagicEntity copy() { return copyResource(); }

	@Override
	public String toString() {
		return "TinyflowKnowledgeMagicResource{id='" + getId() + "', recordType='" + recordType
				+ "', tenantId='" + tenantId + "', knowledgeId='" + knowledgeId + "'}";
	}

	public String getRecordType() { return recordType; }
	public void setRecordType(String recordType) { this.recordType = recordType; }
	public String getTenantId() { return tenantId; }
	public void setTenantId(String tenantId) { this.tenantId = tenantId; }
	public String getKnowledgeId() { return knowledgeId; }
	public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
	public String getProviderKey() { return providerKey; }
	public void setProviderKey(String providerKey) { this.providerKey = providerKey; }
	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public Map<String, Object> getMetadata() { return metadata; }
	public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public long getVersion() { return version; }
	public void setVersion(long version) { this.version = version; }
	public String getVectorStoreKey() { return vectorStoreKey; }
	public void setVectorStoreKey(String vectorStoreKey) { this.vectorStoreKey = vectorStoreKey; }
	public Boolean getMetadataFilterSupported() { return metadataFilterSupported; }
	public void setMetadataFilterSupported(Boolean metadataFilterSupported) { this.metadataFilterSupported = metadataFilterSupported; }
	public String getSourceId() { return sourceId; }
	public void setSourceId(String sourceId) { this.sourceId = sourceId; }
	public String getSourceType() { return sourceType; }
	public void setSourceType(String sourceType) { this.sourceType = sourceType; }
	public String getDocumentId() { return documentId; }
	public void setDocumentId(String documentId) { this.documentId = documentId; }
	public String getChecksum() { return checksum; }
	public void setChecksum(String checksum) { this.checksum = checksum; }
	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }
	public String getSourceUri() { return sourceUri; }
	public void setSourceUri(String sourceUri) { this.sourceUri = sourceUri; }
	public String getSourceVersion() { return sourceVersion; }
	public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
	public String getChunkId() { return chunkId; }
	public void setChunkId(String chunkId) { this.chunkId = chunkId; }
	public String getLocation() { return location; }
	public void setLocation(String location) { this.location = location; }
	public Integer getPosition() { return position; }
	public void setPosition(Integer position) { this.position = position; }
	public String getParentChunkId() { return parentChunkId; }
	public void setParentChunkId(String parentChunkId) { this.parentChunkId = parentChunkId; }
	public Integer getTokenCount() { return tokenCount; }
	public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
	public List<String> getVectorIds() { return vectorIds; }
	public void setVectorIds(List<String> vectorIds) { this.vectorIds = vectorIds; }
	public String getTaskId() { return taskId; }
	public void setTaskId(String taskId) { this.taskId = taskId; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
	public String getCompensationId() { return compensationId; }
	public void setCompensationId(String compensationId) { this.compensationId = compensationId; }
	public String getOperation() { return operation; }
	public void setOperation(String operation) { this.operation = operation; }
	public String getStage() { return stage; }
	public void setStage(String stage) { this.stage = stage; }
	public String getObjectKey() { return objectKey; }
	public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
	public String getAccessKeyId() { return accessKeyId; }
	public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
	public String getPrefix() { return prefix; }
	public void setPrefix(String prefix) { this.prefix = prefix; }
	public String getHashAlgorithm() { return hashAlgorithm; }
	public void setHashAlgorithm(String hashAlgorithm) { this.hashAlgorithm = hashAlgorithm; }
	public String getSalt() { return salt; }
	public void setSalt(String salt) { this.salt = salt; }
	public int getCost() { return cost; }
	public void setCost(int cost) { this.cost = cost; }
	public String getAccessKeyHash() { return accessKeyHash; }
	public void setAccessKeyHash(String accessKeyHash) { this.accessKeyHash = accessKeyHash; }
	public String getConnectionId() { return connectionId; }
	public void setConnectionId(String connectionId) { this.connectionId = connectionId; }
	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
	public String getDatasetId() { return datasetId; }
	public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
	public String getCredentialRef() { return credentialRef; }
	public void setCredentialRef(String credentialRef) { this.credentialRef = credentialRef; }
}
