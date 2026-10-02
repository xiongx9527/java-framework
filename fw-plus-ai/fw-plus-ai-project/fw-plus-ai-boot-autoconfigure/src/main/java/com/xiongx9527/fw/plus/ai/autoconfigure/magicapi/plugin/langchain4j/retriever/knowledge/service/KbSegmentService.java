package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSegmentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbTokenCounter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分段管理服务（设计 §3.1）：列表、编辑（自动重嵌入）、启禁（检索侧以表为权威）、删除（同步删向量）。
 */
public class KbSegmentService {

	private final KbSegmentMapper segmentMapper;

	private final KbDocumentMapper documentMapper;

	private final KbDatasetMapper datasetMapper;

	private final ContentRetrieverContext retrieverContext;

	private final ObjectProvider<KbDocumentService> documentServices;

	private final KbKnowledgeState state;

	public KbSegmentService(KbSegmentMapper segmentMapper, KbDocumentMapper documentMapper,
			KbDatasetMapper datasetMapper, ContentRetrieverContext retrieverContext,
			ObjectProvider<KbDocumentService> documentServices, KbKnowledgeState state) {
		this.segmentMapper = segmentMapper;
		this.documentMapper = documentMapper;
		this.datasetMapper = datasetMapper;
		this.retrieverContext = retrieverContext;
		this.documentServices = documentServices;
		this.state = state;
	}

	public List<KbSegment> listByDocument(String documentId) {
		state.checkReady();
		return segmentMapper.selectList(new QueryWrapper<KbSegment>()
				.eq("document_id", documentId)
				.orderByAsc("position"));
	}

	/**
	 * 编辑分段（含 keywords/answer）：内容变更后自动重嵌入并更新向量（设计 §3.1）。
	 */
	public KbSegment update(String segmentId, String content, String keywords, String answer) {
		state.checkReady();
		KbSegment existing = requireSegment(segmentId);
		KbSegment update = new KbSegment();
		update.setId(segmentId);
		boolean contentChanged = content != null && !content.isEmpty() && !content.equals(existing.getContent());
		if (contentChanged) {
			update.setContent(content);
			update.setWordCount(KbTokenCounter.estimate(content));
			update.setTokens(KbTokenCounter.estimate(content));
			update.setIndexNodeHash(KbIndexingServiceSha.sha256(content));
		}
		if (keywords != null) {
			update.setKeywords(keywords);
		}
		if (answer != null) {
			update.setAnswer(answer);
		}
		update.setUpdateTime(new Date());
		segmentMapper.updateById(update);
		if (contentChanged) {
			reembed(segmentId);
		}
		return segmentMapper.selectById(segmentId);
	}

	/** 启禁用：表为准，检索侧内存过滤兜底（向量 metadata 快照不更新）。 */
	public KbSegment patchEnabled(String segmentId, boolean enabled) {
		state.checkReady();
		requireSegment(segmentId);
		KbSegment update = new KbSegment();
		update.setId(segmentId);
		update.setEnabled(enabled ? "1" : "0");
		update.setUpdateTime(new Date());
		segmentMapper.updateById(update);
		return segmentMapper.selectById(segmentId);
	}

	/** 删除分段：同步删除向量（按 index_node_id，缺省按分段 id）。 */
	public void delete(String segmentId) {
		state.checkReady();
		KbSegment segment = requireSegment(segmentId);
		removeVector(segment);
		segmentMapper.deleteById(segmentId);
	}

	/** 编辑后自动重嵌入：单条重新嵌入并覆盖原向量（index_node_id 不变）。 */
	private void reembed(String segmentId) {
		KbSegment segment = segmentMapper.selectById(segmentId);
		if (segment == null) {
			return;
		}
		KbDataset dataset = datasetMapper.selectById(segment.getDatasetId());
		KbDocument document = documentMapper.selectById(segment.getDocumentId());
		if (dataset == null || document == null) {
			throw new IllegalStateException("分段所属数据集或文档不存在，无法重嵌入");
		}
		String effectiveModel = dataset.getEmbeddingModelActive() != null
				? dataset.getEmbeddingModelActive() : dataset.getEmbeddingModelKey();
		EmbeddingModel embeddingModel = retrieverContext.embeddingModel(effectiveModel);
		EmbeddingStore<TextSegment> store = retrieverContext.embeddingStore(dataset.getEmbeddingStoreKey());
		String nodeId = segment.getIndexNodeId() != null ? segment.getIndexNodeId() : segment.getId();
		try {
			store.remove(nodeId);
		} catch (Throwable ignored) {
			// 向量不存在时忽略
		}
		Map<String, Object> docMetadata = new HashMap<>();
		if (document.getMetadata() != null && !document.getMetadata().isEmpty()) {
			try {
				docMetadata.putAll(com.alibaba.fastjson2.JSON.parseObject(document.getMetadata()));
			} catch (Exception ignored) {
				// 文档 metadata 非法 JSON 时按空处理
			}
		}
		Metadata metadata = new Metadata();
		metadata.put("dataset_id", segment.getDatasetId());
		metadata.put("document_id", segment.getDocumentId());
		metadata.put("segment_id", segment.getId());
		metadata.put("enabled", segment.getEnabled());
		metadata.put("archived", document.getArchived() == null ? "0" : document.getArchived());
		if (segment.getParentId() != null) {
			metadata.put("parent_id", segment.getParentId());
		}
		for (Map.Entry<String, Object> entry : docMetadata.entrySet()) {
			if (entry.getValue() != null) {
				metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
			}
		}
		TextSegment textSegment = TextSegment.from(segment.getContent(), metadata);
		dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(textSegment).content();
		List<String> nodeIds = store.addAll(java.util.Collections.singletonList(embedding),
				java.util.Collections.singletonList(textSegment));
		String newNodeId = nodeIds != null && !nodeIds.isEmpty() ? nodeIds.get(0) : nodeId;
		KbSegment update = new KbSegment();
		update.setId(segmentId);
		update.setIndexNodeId(newNodeId);
		update.setStatus("completed");
		update.setErrorMsg(null);
		update.setUpdateTime(new Date());
		segmentMapper.updateById(update);
	}

	private void removeVector(KbSegment segment) {
		String nodeId = segment.getIndexNodeId() != null ? segment.getIndexNodeId() : segment.getId();
		try {
			KbDataset dataset = datasetMapper.selectById(segment.getDatasetId());
			if (dataset == null) {
				return;
			}
			EmbeddingStore<TextSegment> store = retrieverContext.embeddingStore(dataset.getEmbeddingStoreKey());
			try {
				store.removeAll(java.util.Collections.singletonList(nodeId));
			} catch (Throwable e) {
				try {
					store.remove(nodeId);
				} catch (Throwable ignored) {
					// 单条删除失败不阻断行删除
				}
			}
		} catch (Throwable e) {
			// 向量库不可达时记录但不阻断（残留向量检索时会被跳过）
		}
	}

	private KbSegment requireSegment(String segmentId) {
		KbSegment segment = segmentMapper.selectById(segmentId);
		if (segment == null) {
			throw new IllegalArgumentException("分段不存在: " + segmentId);
		}
		return segment;
	}

	/** 字符串 SHA-256（与索引管道一致的 hash 口径）。 */
	private static class KbIndexingServiceSha {
		static String sha256(String text) {
			return KbIndexingService.sha256(text);
		}
	}
}
