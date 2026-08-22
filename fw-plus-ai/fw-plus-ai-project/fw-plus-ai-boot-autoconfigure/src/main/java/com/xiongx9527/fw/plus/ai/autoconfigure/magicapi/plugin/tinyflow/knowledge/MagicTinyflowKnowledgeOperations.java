package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface MagicTinyflowKnowledgeOperations {

	KnowledgeRecord create(TinyflowKnowledgeDefinition definition);

	KnowledgeRecord update(TinyflowKnowledgeDefinition definition, long expectedVersion);

	KnowledgeRecord find(String tenantId, String knowledgeId);

	PageResult<KnowledgeRecord> listKnowledge(String tenantId, int page, int size);

	void delete(String tenantId, String knowledgeId, long expectedVersion);

	TinyflowIngestionTask importDocument(ImportCommand command);

	PageResult<TinyflowChunk> listChunks(String tenantId, String knowledgeId, String documentId,
			int page, int size);

	TinyflowChunk updateChunk(String tenantId, String knowledgeId, String chunkId, boolean enabled,
			String content, Map<String, Object> metadata);

	PageResult<TinyflowIngestionTask> listTasks(String tenantId, String knowledgeId, int page, int size);

	TinyflowIngestionTask retry(String tenantId, String knowledgeId, String taskId);

	TinyflowIngestionTask cancel(String tenantId, String knowledgeId, String taskId);

	TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request);

	final class KnowledgeRecord {
		private final TinyflowKnowledgeDefinition definition;
		private final long version;

		public KnowledgeRecord(TinyflowKnowledgeDefinition definition, long version) {
			if (definition == null) throw new IllegalArgumentException("definition must not be null");
			if (version < 0L) throw new IllegalArgumentException("version must not be negative");
			this.definition = definition;
			this.version = version;
		}

		public TinyflowKnowledgeDefinition getDefinition() { return definition; }
		public long getVersion() { return version; }
	}

	final class PageResult<T> {
		private final int page;
		private final int size;
		private final long total;
		private final List<T> items;

		public PageResult(int page, int size, long total, List<T> items) {
			if (page < 1 || size < 1 || total < 0L) throw new IllegalArgumentException("page result is invalid");
			this.page = page;
			this.size = size;
			this.total = total;
			this.items = items == null ? Collections.<T>emptyList()
					: Collections.unmodifiableList(new ArrayList<T>(items));
		}

		public int getPage() { return page; }
		public int getSize() { return size; }
		public long getTotal() { return total; }
		public List<T> getItems() { return items; }
	}

	final class ImportCommand {
		private final String tenantId;
		private final String knowledgeId;
		private final String sourceId;
		private final String sourceType;
		private final String name;
		private final String mediaType;
		private final byte[] content;
		private final String sourceUri;
		private final Map<String, Object> options;

		public ImportCommand(String tenantId, String knowledgeId, String sourceId, String sourceType,
				String name, String mediaType, byte[] content, String sourceUri, Map<String, Object> options) {
			this.tenantId = required(tenantId, "tenantId");
			this.knowledgeId = required(knowledgeId, "knowledgeId");
			this.sourceId = required(sourceId, "sourceId");
			this.sourceType = required(sourceType, "sourceType");
			this.name = required(name, "name");
			this.mediaType = required(mediaType, "mediaType");
			if (content == null) throw new IllegalArgumentException("content must not be null");
			this.content = content.clone();
			this.sourceUri = sourceUri;
			this.options = options == null ? Collections.<String, Object>emptyMap()
					: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(options));
		}

		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getSourceId() { return sourceId; }
		public String getSourceType() { return sourceType; }
		public String getName() { return name; }
		public String getMediaType() { return mediaType; }
		public byte[] getContent() { return content.clone(); }
		public String getSourceUri() { return sourceUri; }
		public Map<String, Object> getOptions() { return options; }

		private static String required(String value, String field) {
			if (value == null || value.trim().length() == 0) {
				throw new IllegalArgumentException(field + " must not be blank");
			}
			return value.trim();
		}
	}
}
