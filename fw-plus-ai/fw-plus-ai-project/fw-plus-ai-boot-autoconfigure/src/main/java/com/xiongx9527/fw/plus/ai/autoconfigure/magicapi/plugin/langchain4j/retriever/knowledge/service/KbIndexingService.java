package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSegmentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.DocumentPipeline;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbIndexingExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbTokenCounter;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.ParentChildSplitter;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store.KbOriginalFileStore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 索引管道服务（设计 §2.3）：入库与索引执行解耦（数据库即队列），CAS 认领保证多实例不重复索引；
 * 状态机 waiting → parsing → splitting → indexing → completed | error，实时写回。
 */
public class KbIndexingService {

	private static final Logger logger = LoggerFactory.getLogger(KbIndexingService.class);

	private static final int POLL_BATCH = 10;

	private static final int EMBED_BATCH_SIZE = 16;

	private final KbDatasetMapper datasetMapper;

	private final KbDocumentMapper documentMapper;

	private final KbSegmentMapper segmentMapper;

	private final KbOriginalFileStore fileStore;

	private final DocumentPipeline documentPipeline;

	private final KbIndexingExecutor executor;

	private final ContentRetrieverContext retrieverContext;

	private final KbKnowledgeState state;

	public KbIndexingService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper, KbOriginalFileStore fileStore, DocumentPipeline documentPipeline,
			KbIndexingExecutor executor, ContentRetrieverContext retrieverContext, KbKnowledgeState state) {
		this.datasetMapper = datasetMapper;
		this.documentMapper = documentMapper;
		this.segmentMapper = segmentMapper;
		this.fileStore = fileStore;
		this.documentPipeline = documentPipeline;
		this.executor = executor;
		this.retrieverContext = retrieverContext;
		this.state = state;
	}

	/** 入库即时触发（设计 §2.3：入库只置 waiting，执行方认领处理）。 */
	public void trigger(KbDocument document) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				processOne(document.getId());
			}
		});
	}

	/** waiting 轮询兜底：扫描后逐个认领（认领失败的文档说明其它实例已在处理）。 */
	public void pollWaiting() {
		if (!state.canProcess()) {
			return;
		}
		try {
			List<KbDocument> waiting = documentMapper.selectList(new QueryWrapper<KbDocument>()
					.eq("indexing_status", "waiting")
					.last("LIMIT " + POLL_BATCH));
			for (final KbDocument document : waiting) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						processOne(document.getId());
					}
				});
			}
		} catch (Throwable e) {
			logger.warn("知识库 waiting 扫描失败: {}", e.getMessage());
		}
	}

	/**
	 * 处理单个文档：CAS 认领 → 解析 → 切分 → 嵌入 → completed/error。
	 *
	 * @return 是否由本次调用完成索引（false 表示已被其它实例认领）
	 */
	public boolean processOne(String documentId) {
		try {
			state.checkReady();
		} catch (IllegalStateException e) {
			logger.warn("跳过文档索引（知识库未就绪）: {}", e.getMessage());
			return false;
		}
		KbDocument document = documentMapper.selectById(documentId);
		if (document == null) {
			return false;
		}
		// CAS 认领：仅 waiting 状态可进入 parsing（多实例部署下同一文档不会被重复索引）
		if (documentMapper.casTransit(documentId, "waiting", "parsing") == 0) {
			return false;
		}
		long start = System.currentTimeMillis();
		try {
			KbDataset dataset = datasetMapper.selectById(document.getDatasetId());
			if (dataset == null) {
				throw new IllegalStateException("所属数据集不存在: " + document.getDatasetId());
			}
			// 1. 解析
			String text = parseOriginal(document);
			// 2. 切分
			documentMapper.casTransit(documentId, "parsing", "splitting");
			List<KbSegment> segments = split(dataset, document, text);
			// 3. 嵌入
			documentMapper.casTransit(documentId, "splitting", "indexing");
			embedAndStore(dataset, segments);
			// 4. 完成
			documentMapper.casTransit(documentId, "indexing", "completed");
			// error_msg 用 UpdateWrapper 显式置 null（updateById 忽略 null 字段，旧错误信息会残留）
			documentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<KbDocument>()
					.eq("id", documentId)
					.set("error_msg", null)
					.set("latency_ms", System.currentTimeMillis() - start)
					.set("word_count", approxWordCount(text))
					.set("update_time", new Date()));
			return true;
		} catch (Throwable e) {
			logger.error("文档索引失败: {}", documentId, e);
			KbDocument error = new KbDocument();
			error.setId(documentId);
			error.setIndexingStatus("error");
			error.setErrorMsg(truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
			error.setUpdateTime(new Date());
			documentMapper.updateById(error);
			return false;
		}
	}

	/** 重建索引（设计 §2.3）：删旧分段与向量，回读原始文件重新入队。 */
	public void reindex(String documentId) {
		state.checkReady();
		KbDocument document = documentMapper.selectById(documentId);
		if (document == null) {
			throw new IllegalArgumentException("文档不存在: " + documentId);
		}
		deleteSegmentsAndVectors(documentId);
		KbDocument update = new KbDocument();
		update.setId(documentId);
		update.setIndexingStatus("waiting");
		update.setErrorMsg(null);
		update.setLatencyMs(null);
		update.setUpdateTime(new Date());
		documentMapper.updateById(update);
		trigger(document);
	}

	/** 删除文档的全部分段行与向量（store 能力不足时降级逐条删除）。 */
	public void deleteSegmentsAndVectors(String documentId) {
		List<KbSegment> segments = segmentMapper.selectList(new QueryWrapper<KbSegment>()
				.eq("document_id", documentId));
		List<String> nodeIds = new ArrayList<>();
		for (KbSegment segment : segments) {
			nodeIds.add(segment.getIndexNodeId() != null ? segment.getIndexNodeId() : segment.getId());
		}
		removeVectors(documentMapper.selectById(documentId), nodeIds);
		for (KbSegment segment : segments) {
			segmentMapper.deleteById(segment.getId());
		}
	}

	/** 删除向量：removeAll 批量优先，不支持时逐条 remove 降级。 */
	public void removeVectors(KbDocument document, List<String> nodeIds) {
		if (nodeIds.isEmpty() || document == null) {
			return;
		}
		try {
			KbDataset dataset = datasetMapper.selectById(document.getDatasetId());
			if (dataset == null) {
				return;
			}
			EmbeddingStore<TextSegment> store = retrieverContext.embeddingStore(dataset.getEmbeddingStoreKey());
			try {
				store.removeAll(nodeIds);
			} catch (Throwable e) {
				// store 不支持按 ids 批量删除：逐条降级
				for (String nodeId : nodeIds) {
					try {
						store.remove(nodeId);
					} catch (Throwable ignored) {
						// 单条失败不阻断（可能向量本就不存在）
					}
				}
			}
		} catch (Throwable e) {
			// 向量库不可达时记录但不阻断行删除（残留向量检索时会被跳过）
			logger.warn("删除文档向量失败（documentId={}）: {}", document.getId(), e.getMessage());
		}
	}

	/** 解析原文（upload 与 create-by-text 统一回读存储的原文）。 */
	private String parseOriginal(KbDocument document) {
		if (document.getFilePath() == null || document.getFilePath().isEmpty()) {
			throw new IllegalStateException("原始文件路径为空，无法解析: " + document.getId());
		}
		InputStream in = fileStore.read(document.getFilePath());
		return documentPipeline.parse(in);
	}

	private List<KbSegment> split(KbDataset dataset, KbDocument document, String text) {
		String mode = dataset.getSegmentMode() == null ? "general" : dataset.getSegmentMode();
		int maxLen = dataset.getSegmentMaxLength() != null ? dataset.getSegmentMaxLength() : 1000;
		int overlap = dataset.getSegmentOverlap() != null ? dataset.getSegmentOverlap() : 200;
		String delimiter = dataset.getSegmentDelimiter();

		List<KbSegment> segments = new ArrayList<>();
		Date now = new Date();
		if ("parent-child".equals(mode)) {
			ParentChildSplitter splitter = new ParentChildSplitter(maxLen, overlap);
			List<ParentChildSplitter.Piece> pieces = splitter.split(text, delimiter);
			Map<String, KbSegment> parents = new java.util.LinkedHashMap<>();
			for (ParentChildSplitter.Piece piece : pieces) {
				KbSegment parent = parents.get(piece.getParentContent());
				if (parent == null) {
					// 父段：仅落表，不嵌入（设计 §2.3）
					parent = newSegment(dataset, document, piece.getParentPosition(),
							piece.getParentContent(), now);
					parent.setParentId(null);
					parent.setStatus("completed");
					segmentMapper.insert(parent);
					parents.put(piece.getParentContent(), parent);
				}
				KbSegment child = newSegment(dataset, document, piece.getChildPosition(),
						piece.getChildContent(), now);
				child.setParentId(parent.getId());
				segmentMapper.insert(child);
				segments.add(child);
			}
		} else if ("qa".equals(mode)) {
			// QA 模式（设计 §2.3）：切父段 → 每父段 prompt 抽取 {question, answer} 生成 QA 分段（嵌入 question）
			if (dataset.getChatModelKey() == null || dataset.getChatModelKey().isEmpty()) {
				throw new IllegalStateException("QA 分段模式需要在数据集配置 chat_model_key（对话模型资源 key）");
			}
			dev.langchain4j.model.chat.ChatModel chatModel =
					retrieverContext.chatModel(dataset.getChatModelKey());
			List<String> parentChunks = documentPipeline.splitGeneral(text, maxLen, overlap, delimiter);
			int qaPosition = 0;
			for (String parent : parentChunks) {
				for (String[] qa : com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j
						.retriever.knowledge.pipeline.QaExtractor.extract(chatModel, parent)) {
					KbSegment segment = newSegment(dataset, document, qaPosition++, qa[0], now);
					segment.setAnswer(qa[1]);
					segmentMapper.insert(segment);
					segments.add(segment);
				}
			}
		} else {
			List<String> chunks = documentPipeline.splitGeneral(text, maxLen, overlap, delimiter);
			for (int i = 0; i < chunks.size(); i++) {
				KbSegment segment = newSegment(dataset, document, i, chunks.get(i), now);
				segmentMapper.insert(segment);
				segments.add(segment);
			}
		}
		return segments;
	}

	private KbSegment newSegment(KbDataset dataset, KbDocument document, int position, String content, Date now) {
		KbSegment segment = new KbSegment();
		segment.setId(UUID.randomUUID().toString().replace("-", ""));
		segment.setDatasetId(dataset.getId());
		segment.setDocumentId(document.getId());
		segment.setPosition(position);
		segment.setContent(content);
		segment.setWordCount(approxWordCount(content));
		segment.setTokens(KbTokenCounter.estimate(content));
		segment.setStatus("waiting");
		segment.setEnabled("1");
		segment.setHitCount(0L);
		segment.setCreateTime(now);
		segment.setUpdateTime(now);
		return segment;
	}

	/**
	 * 批量嵌入并写入向量库：EmbeddingStore.addAll 批量写入；嵌入模型取 embedding_model_active
	 * （空则取配置模型并在成功后更新 active），metadata 携带 dataset_id/document_id/segment_id/enabled/archived。
	 */
	private void embedAndStore(KbDataset dataset, List<KbSegment> segments) {
		if (segments.isEmpty()) {
			return;
		}
		// economy 经济模式：不入向量库，分段直接 completed（检索走关键词路，设计 §2.3 预留项）
		if ("economy".equalsIgnoreCase(dataset.getIndexingTechnique())) {
			for (KbSegment segment : segments) {
				KbSegment update = new KbSegment();
				update.setId(segment.getId());
				update.setStatus("completed");
				update.setUpdateTime(new Date());
				segmentMapper.updateById(update);
			}
			return;
		}
		String effectiveModel = dataset.getEmbeddingModelActive() != null
				? dataset.getEmbeddingModelActive()
				: dataset.getEmbeddingModelKey();
		EmbeddingModel embeddingModel = retrieverContext.embeddingModel(effectiveModel);
		EmbeddingStore<TextSegment> store = retrieverContext.embeddingStore(dataset.getEmbeddingStoreKey());

		KbDocument document = documentMapper.selectById(segments.get(0).getDocumentId());
		Map<String, Object> docMetadata = new HashMap<>();
		if (document != null && document.getMetadata() != null && !document.getMetadata().isEmpty()) {
			try {
				docMetadata.putAll(com.alibaba.fastjson2.JSON.parseObject(document.getMetadata()));
			} catch (Exception ignored) {
				// 文档 metadata 非法 JSON 时按空处理
			}
		}

		List<String> ids = new ArrayList<>();
		List<Embedding> embeddings = new ArrayList<>();
		List<TextSegment> textSegments = new ArrayList<>();
		for (int start = 0; start < segments.size(); start += EMBED_BATCH_SIZE) {
			embeddings.clear();
			textSegments.clear();
			List<KbSegment> batch = segments.subList(start, Math.min(start + EMBED_BATCH_SIZE, segments.size()));
			for (KbSegment segment : batch) {
				textSegments.add(toVectorSegment(segment, docMetadata));
			}
			List<Embedding> embedded = embeddingModel.embedAll(textSegments).content();
			embeddings.addAll(embedded);
			// 0.35.0 无法指定向量 id：indexNodeId 取 store 生成的 id 回填
			List<String> nodeIds = store.addAll(embeddings, textSegments);
			for (int i = 0; i < batch.size(); i++) {
				KbSegment segment = batch.get(i);
				KbSegment update = new KbSegment();
				update.setId(segment.getId());
				update.setIndexNodeId(nodeIds != null && i < nodeIds.size() ? nodeIds.get(i) : segment.getId());
				update.setIndexNodeHash(sha256(segment.getContent()));
				update.setStatus("completed");
				update.setUpdateTime(new Date());
				segmentMapper.updateById(update);
			}
		}

		// 嵌入一致性：首次索引成功后回填 embedding_model_active（设计 §2.3）
		if (dataset.getEmbeddingModelActive() == null || dataset.getEmbeddingModelActive().isEmpty()) {
			KbDataset update = new KbDataset();
			update.setId(dataset.getId());
			update.setEmbeddingModelActive(effectiveModel);
			update.setUpdateTime(new Date());
			datasetMapper.updateById(update);
		}
	}

	private TextSegment toVectorSegment(KbSegment segment, Map<String, Object> docMetadata) {
		Metadata metadata = new Metadata();
		metadata.put("dataset_id", segment.getDatasetId());
		metadata.put("document_id", segment.getDocumentId());
		metadata.put("segment_id", segment.getId());
		metadata.put("enabled", segment.getEnabled());
		metadata.put("archived", "0");
		if (segment.getParentId() != null) {
			metadata.put("parent_id", segment.getParentId());
		}
		for (Map.Entry<String, Object> entry : docMetadata.entrySet()) {
			if (entry.getValue() != null) {
				metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
			}
		}
		return TextSegment.from(segment.getContent(), metadata);
	}

	private static int approxWordCount(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		int count = 0;
		boolean inWord = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isWhitespace(c)) {
				inWord = false;
			} else if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
				count++;
				inWord = false;
			} else if (!inWord) {
				count++;
				inWord = true;
			}
		}
		return count;
	}

	static String sha256(String text) {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (byte b : hash) {
				builder.append(String.format("%02x", b));
			}
			return builder.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 不可用", e);
		}
	}

	private static String truncate(String message) {
		return message.length() > 2000 ? message.substring(0, 2000) : message;
	}
}
