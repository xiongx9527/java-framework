package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSetting;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSegmentMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库数据集服务（设计 §3.1）：CRUD、删除级联校验（knowledge retriever 引用提示）、召回测试与
 * retriever 引用查询。嵌入模型切换后提示重建索引（embedding_model_active 与配置不一致时）。
 */
public class KbDatasetService {

	private final KbDatasetMapper datasetMapper;

	private final KbDocumentMapper documentMapper;

	private final KbSegmentMapper segmentMapper;

	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.DocumentPipeline documentPipeline;

	private final KbSearchService searchService;

	private final KbIndexingService indexingService;

	private final ObjectProvider<MagicResourceService> resourceServices;

	private final ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.resource.Langchain4jContentRetrieverMagicResourceStorage> retrieverStorages;

	private final KbKnowledgeState state;

	/** 全局设置：提供新建数据集时的模型默认值（每次现读，改完设置立即影响下一个新建的数据集） */
	private final KbSettingService settings;

	public KbDatasetService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.DocumentPipeline documentPipeline,
			KbSearchService searchService, KbIndexingService indexingService,
			ObjectProvider<MagicResourceService> resourceServices,
			ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.resource.Langchain4jContentRetrieverMagicResourceStorage> retrieverStorages,
			KbKnowledgeState state, KbSettingService settings) {
		this.datasetMapper = datasetMapper;
		this.documentMapper = documentMapper;
		this.segmentMapper = segmentMapper;
		this.documentPipeline = documentPipeline;
		this.searchService = searchService;
		this.indexingService = indexingService;
		this.resourceServices = resourceServices;
		this.retrieverStorages = retrieverStorages;
		this.state = state;
		this.settings = settings;
	}

	public List<KbDataset> list() {
		state.checkReady();
		return datasetMapper.selectList(new QueryWrapper<KbDataset>().orderByDesc("create_time"));
	}

	public KbDataset get(String id) {
		state.checkReady();
		return requireDataset(id);
	}

	public KbDataset create(KbDataset dataset) {
		state.checkReady();
		// 先补全局默认再校验：页面只配一次，建数据集时留空即可
		applyDatasetDefaults(dataset, null);
		validate(dataset);
		if (dataset.getId() == null || dataset.getId().isEmpty()) {
			dataset.setId(UUID.randomUUID().toString().replace("-", ""));
		}
		// active 初始等于配置值：这样后续换模型时 update 的「已切换」提示才生效
		// （原实现留空，active 为 null 时该分支被跳过，第一次换模型不会提示重建索引）
		if (isBlank(dataset.getEmbeddingModelActive())) {
			dataset.setEmbeddingModelActive(dataset.getEmbeddingModelKey());
		}
		if (isBlank(dataset.getStatus())) {
			dataset.setStatus("active");
		}
		Date now = new Date();
		dataset.setCreateTime(now);
		dataset.setUpdateTime(now);
		dataset.setDelFlag("0");
		datasetMapper.insert(dataset);
		return dataset;
	}

	public KbDataset update(String id, KbDataset dataset) {
		state.checkReady();
		KbDataset existing = requireDataset(id);
		validate(dataset);
		String newModelKey = dataset.getEmbeddingModelKey() != null
				? dataset.getEmbeddingModelKey() : existing.getEmbeddingModelKey();
		KbDataset update = new KbDataset();
		update.setId(id);
		update.setName(dataset.getName() != null ? dataset.getName() : existing.getName());
		update.setDescription(dataset.getDescription() != null ? dataset.getDescription() : existing.getDescription());
		update.setIndexingTechnique(dataset.getIndexingTechnique() != null
				? dataset.getIndexingTechnique() : existing.getIndexingTechnique());
		update.setEmbeddingModelKey(newModelKey);
		update.setEmbeddingStoreKey(dataset.getEmbeddingStoreKey() != null
				? dataset.getEmbeddingStoreKey() : existing.getEmbeddingStoreKey());
		update.setScoringModelKey(dataset.getScoringModelKey() != null
				? dataset.getScoringModelKey() : existing.getScoringModelKey());
		update.setChatModelKey(dataset.getChatModelKey() != null
				? dataset.getChatModelKey() : existing.getChatModelKey());
		update.setCollectionName(dataset.getCollectionName() != null
				? dataset.getCollectionName() : existing.getCollectionName());
		update.setRetrievalTopK(dataset.getRetrievalTopK() != null
				? dataset.getRetrievalTopK() : existing.getRetrievalTopK());
		update.setRetrievalScoreThreshold(dataset.getRetrievalScoreThreshold() != null
				? dataset.getRetrievalScoreThreshold() : existing.getRetrievalScoreThreshold());
		update.setSegmentMaxLength(dataset.getSegmentMaxLength() != null
				? dataset.getSegmentMaxLength() : existing.getSegmentMaxLength());
		update.setSegmentOverlap(dataset.getSegmentOverlap() != null
				? dataset.getSegmentOverlap() : existing.getSegmentOverlap());
		update.setSegmentDelimiter(dataset.getSegmentDelimiter() != null
				? dataset.getSegmentDelimiter() : existing.getSegmentDelimiter());
		update.setSegmentMode(dataset.getSegmentMode() != null ? dataset.getSegmentMode() : existing.getSegmentMode());
		update.setStatus(dataset.getStatus() != null ? dataset.getStatus() : existing.getStatus());
		update.setUpdateTime(new Date());
		// 回落顺序：本次提交 > 数据集现有值 > 全局设置默认（老数据集建在设置页之前，字段可能为空）
		applyDatasetDefaults(update, existing);
		// 嵌入模型切换提示重建索引（设计 §2.3：active 与配置不一致时）
		if (existing.getEmbeddingModelActive() != null && !existing.getEmbeddingModelActive().isEmpty()
				&& !existing.getEmbeddingModelActive().equals(newModelKey)) {
			update.setErrorMsg("嵌入模型已切换（" + existing.getEmbeddingModelActive() + " → " + newModelKey
					+ "），请对文档执行重建索引以保持向量一致");
		}
		datasetMapper.updateById(update);
		return datasetMapper.selectById(id);
	}

	/**
	 * 删除数据集：级联删除全部文档（分段/向量/原始文件）。
	 *
	 * @return 引用的 knowledge retriever 资源 key 列表（调用方展示提示，不阻断删除）
	 */
	public List<String> delete(String id) {
		state.checkReady();
		requireDataset(id);
		List<String> referencedBy = referencedRetrievers(id);
		List<KbDocument> documents = documentMapper.selectList(new QueryWrapper<KbDocument>()
				.eq("dataset_id", id));
		for (KbDocument document : documents) {
			indexingService.deleteSegmentsAndVectors(document.getId());
		}
		documentMapper.delete(new QueryWrapper<KbDocument>().eq("dataset_id", id));
		datasetMapper.deleteById(id);
		return referencedBy;
	}

	/**
	 * 数据集下的文档分页列表（管理端文档视图）：手动 LIMIT/OFFSET 分页，
	 * 不依赖 MyBatis-Plus 分页插件配置。
	 *
	 * @return {list, total, page, pageSize}
	 */
	public Map<String, Object> listDocuments(String datasetId, int page, int pageSize) {
		state.checkReady();
		requireDataset(datasetId);
		int safePage = Math.max(1, page);
		int safeSize = Math.min(100, Math.max(1, pageSize));
		// MP 标准分页（PaginationInterceptor 由 fw-plus-boot mybatisplus 插件保证注册）
		com.baomidou.mybatisplus.extension.plugins.pagination.Page<KbDocument> pageRequest =
				new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(safePage, safeSize);
		com.baomidou.mybatisplus.core.metadata.IPage<KbDocument> result =
				documentMapper.selectPage(pageRequest, new QueryWrapper<KbDocument>()
						.eq("dataset_id", datasetId)
						.orderByDesc("create_time"));
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("list", result.getRecords());
		response.put("total", result.getTotal());
		response.put("page", safePage);
		response.put("pageSize", safeSize);
		return response;
	}

	/** 召回测试（设计 §3.1）：query → 嵌入 → 向量检索 → 过滤 → rerank → 命中分段与 score。 */
	public List<KbSearchHit> hitTest(String id, String query, Integer topK, Double minScore,
			JSONObject metadataCondition, String documentId) {
		state.checkReady();
		requireDataset(id);
		return searchService.search(id, query, topK, minScore, metadataCondition, documentId);
	}

	/**
	 * 引用该数据集的 knowledge 类型 retriever 资源列表（设计 §3.1）：
	 * 扫描 langchain4j-content-retriever 资源，options.dataset-id 匹配即引用。
	 */
	public List<Map<String, Object>> referencedRetrieverResources(String id) {
		state.checkReady();
		requireDataset(id);
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map.Entry<String, String> entry : referencedRetrieverKeys(id).entrySet()) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("key", entry.getKey());
			item.put("name", entry.getValue());
			result.add(item);
		}
		return result;
	}

	private List<String> referencedRetrievers(String id) {
		return new ArrayList<>(referencedRetrieverKeys(id).keySet());
	}

	private Map<String, String> referencedRetrieverKeys(String datasetId) {
		Map<String, String> keys = new LinkedHashMap<>();
		MagicResourceService resourceService = resourceServices == null ? null : resourceServices.getIfAvailable();
		com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.resource.Langchain4jContentRetrieverMagicResourceStorage storage =
				retrieverStorages == null ? null : retrieverStorages.getIfAvailable();
		if (resourceService == null || storage == null) {
			return keys;
		}
		try {
			List<?> files = resourceService.listFiles(storage.folder());
			for (Object file : files) {
				if (file instanceof com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo) {
					com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo info =
							(com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo) file;
					if (info.getOptions() == null) {
						continue;
					}
					Object datasetRef = info.getOptions().get("dataset-id");
					if (datasetId.equals(datasetRef)) {
						keys.put(info.getKey(), info.getName() == null ? info.getKey() : info.getName());
					}
				}
			}
		} catch (Exception e) {
			// 资源服务不可用时按无引用处理（管理端展示空列表）
		}
		return keys;
	}

	/**
	 * 补齐数据集的空值字段：模型 key 与切分/召回参数，取自全局设置页配的默认值。
	 *
	 * <p>回落顺序为「传入值 → {@code fallback}（更新场景下的数据集现有值）→ 全局设置 → 硬编码兜底」。
	 * 只填空值，绝不覆盖调用方显式给的内容——设置页改的是<b>新建数据集</b>的默认，
	 * 不该回头篡改已有数据集的模型绑定（那会让已建好的向量与配置悄悄脱节）。
	 *
	 * @param dataset  待补齐的数据集（原地修改）
	 * @param fallback 更新场景下的现有数据集，可为 null
	 *
	 * <p>包级可见以便 {@code KbDatasetDefaultsTest} 直接覆盖回落顺序。
	 */
	void applyDatasetDefaults(KbDataset dataset, KbDataset fallback) {
		KbSetting current = currentSetting();
		dataset.setEmbeddingModelKey(firstNonBlank(dataset.getEmbeddingModelKey(),
				fallback == null ? null : fallback.getEmbeddingModelKey(),
				current == null ? null : current.getEmbeddingModelKey()));
		dataset.setEmbeddingStoreKey(firstNonBlank(dataset.getEmbeddingStoreKey(),
				fallback == null ? null : fallback.getEmbeddingStoreKey(),
				current == null ? null : current.getEmbeddingStoreKey()));
		dataset.setScoringModelKey(firstNonBlank(dataset.getScoringModelKey(),
				fallback == null ? null : fallback.getScoringModelKey(),
				current == null ? null : current.getScoringModelKey()));
		dataset.setChatModelKey(firstNonBlank(dataset.getChatModelKey(),
				fallback == null ? null : fallback.getChatModelKey(),
				current == null ? null : current.getChatModelKey()));
		// 切分/召回参数：设置页也允许留空，留空才落到原有的硬编码兜底，
		// 保证「设置表一行都没配」时行为与改动前完全一致
		dataset.setRetrievalTopK(firstNonNull(dataset.getRetrievalTopK(),
				fallback == null ? null : fallback.getRetrievalTopK(),
				current == null ? null : current.getDefaultRetrievalTopK(),
				KbSettingService.DEFAULT_RETRIEVAL_TOP_K));
		dataset.setRetrievalScoreThreshold(firstNonNull(dataset.getRetrievalScoreThreshold(),
				fallback == null ? null : fallback.getRetrievalScoreThreshold(),
				current == null ? null : current.getDefaultRetrievalScoreThreshold(),
				new java.math.BigDecimal("0.0000")));
		dataset.setSegmentMaxLength(firstNonNull(dataset.getSegmentMaxLength(),
				fallback == null ? null : fallback.getSegmentMaxLength(),
				current == null ? null : current.getDefaultSegmentMaxLength(),
				KbSettingService.DEFAULT_SEGMENT_MAX_LENGTH));
		dataset.setSegmentOverlap(firstNonNull(dataset.getSegmentOverlap(),
				fallback == null ? null : fallback.getSegmentOverlap(),
				current == null ? null : current.getDefaultSegmentOverlap(),
				KbSettingService.DEFAULT_SEGMENT_OVERLAP));
		// 分隔符与分段模式按「非空」判定（空串与 null 同义：都用默认）
		dataset.setSegmentDelimiter(firstNonBlank(dataset.getSegmentDelimiter(),
				fallback == null ? null : fallback.getSegmentDelimiter(),
				current == null ? null : current.getDefaultSegmentDelimiter()));
		dataset.setSegmentMode(firstNonBlank(dataset.getSegmentMode(),
				fallback == null ? null : fallback.getSegmentMode(),
				current == null ? null : current.getDefaultSegmentMode(),
				KbSettingService.SEGMENT_MODE_GENERAL));
		dataset.setIndexingTechnique(firstNonBlank(dataset.getIndexingTechnique(),
				fallback == null ? null : fallback.getIndexingTechnique(),
				current == null ? null : current.getDefaultIndexingTechnique(),
				KbSettingService.INDEXING_TECHNIQUE_HIGH_QUALITY));
	}

	/** 取全局设置快照；未注入或读表异常时返回 null（不阻断，交给 validate 报明确错误）。 */
	private KbSetting currentSetting() {
		if (settings == null) {
			return null;
		}
		try {
			return settings.current();
		} catch (Throwable e) {
			return null;
		}
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (!isBlank(value)) {
				return value;
			}
		}
		return null;
	}

	/** 取第一个非 null 的值（数值项没有「空串」概念，null 即未提供）。 */
	private static <T> T firstNonNull(T... values) {
		for (T value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private void validate(KbDataset dataset) {
		if (isBlank(dataset.getName())) {
			throw new IllegalArgumentException("数据集名称不能为空");
		}
		if (isBlank(dataset.getEmbeddingModelKey())) {
			throw new IllegalArgumentException("嵌入模型资源 key（embedding-model-key）不能为空；"
					+ "可在知识库控制台「设置」页配置新建数据集的默认值，之后留空即可");
		}
		if (isBlank(dataset.getEmbeddingStoreKey())) {
			throw new IllegalArgumentException("向量库资源 key（embedding-store-key）不能为空；"
					+ "可在知识库控制台「设置」页配置新建数据集的默认值，之后留空即可");
		}
		String mode = dataset.getSegmentMode();
		if (!isBlank(mode) && !KbSettingService.SEGMENT_MODES.contains(mode)) {
			throw new IllegalArgumentException("不支持的分段模式: " + mode + "（general / parent-child / qa）");
		}
		if (KbSettingService.SEGMENT_MODE_QA.equals(mode) && isBlank(dataset.getChatModelKey())) {
			throw new IllegalArgumentException("QA 分段模式需要配置对话模型资源 key（chat-model-key）；"
					+ "可在知识库控制台「设置」页配置新建数据集的默认值");
		}
		String technique = dataset.getIndexingTechnique();
		if (!isBlank(technique) && !KbSettingService.INDEXING_TECHNIQUES.contains(technique)) {
			throw new IllegalArgumentException("不支持的索引技术: " + technique
					+ "（high_quality / economy）");
		}
	}

	private KbDataset requireDataset(String id) {
		KbDataset dataset = datasetMapper.selectById(id);
		if (dataset == null) {
			throw new IllegalArgumentException("知识库数据集不存在: " + id);
		}
		return dataset;
	}

	// ---------------- 切分预览 / 导出 / 导入 ----------------

	/**
	 * 切分预览（Dry-run）：按数据集配置（或显式参数）执行切分器，返回分段文本与 token 估计，
	 * 不落库、不嵌入。QA 模式需调用对话模型生成，不做预览。
	 */
	public List<Map<String, Object>> previewSplit(String text, String segmentMode, Integer segmentMaxLength,
			Integer segmentOverlap, String segmentDelimiter) {
		state.checkReady();
		int maxLen = segmentMaxLength != null && segmentMaxLength > 0 ? segmentMaxLength : 1000;
		int overlap = segmentOverlap != null && segmentOverlap >= 0 ? segmentOverlap : 200;
		String mode = segmentMode == null || segmentMode.isEmpty() ? "general" : segmentMode;
		List<Map<String, Object>> preview = new ArrayList<>();
		if ("parent-child".equals(mode)) {
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.ParentChildSplitter
					splitter = new com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.ParentChildSplitter(
					maxLen, overlap);
			int parentPosition = -1;
			String lastParent = null;
			for (com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.ParentChildSplitter.Piece
					piece : splitter.split(text, segmentDelimiter)) {
				if (!piece.getParentContent().equals(lastParent)) {
					parentPosition++;
					lastParent = piece.getParentContent();
					// 父段预览（父段落表不嵌入）
					Map<String, Object> parent = new LinkedHashMap<>();
					parent.put("position", parentPosition);
					parent.put("content", piece.getParentContent());
					parent.put("tokens", com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbTokenCounter
							.estimate(piece.getParentContent()));
					parent.put("parent", true);
					preview.add(parent);
				}
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("position", piece.getChildPosition());
				item.put("content", piece.getChildContent());
				item.put("tokens", com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbTokenCounter
						.estimate(piece.getChildContent()));
				item.put("parent", false);
				item.put("parentId", parentPosition);
				preview.add(item);
			}
			return preview;
		}
		List<String> chunks = documentPipeline.splitGeneral(text, maxLen, overlap, segmentDelimiter);
		for (int i = 0; i < chunks.size(); i++) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("position", i);
			item.put("content", chunks.get(i));
			item.put("tokens", com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbTokenCounter
					.estimate(chunks.get(i)));
			preview.add(item);
		}
		return preview;
	}

	/**
	 * 导出数据集（不含向量）：JSON {dataset, documents:[{...document, segments:[...]}]}，
	 * 导入方按文档 doc_hash 与分段内容重建索引（重新嵌入）。
	 */
	public com.alibaba.fastjson2.JSONObject export(String datasetId) {
		state.checkReady();
		KbDataset dataset = requireDataset(datasetId);
		com.alibaba.fastjson2.JSONObject payload = new com.alibaba.fastjson2.JSONObject();
		payload.put("version", 1);
		payload.put("dataset", JSON.parseObject(JSON.toJSONString(dataset)));
		com.alibaba.fastjson2.JSONArray documents = new com.alibaba.fastjson2.JSONArray();
		for (KbDocument document : documentMapper.selectList(new QueryWrapper<KbDocument>()
				.eq("dataset_id", datasetId).orderByAsc("create_time"))) {
			com.alibaba.fastjson2.JSONObject docJson = JSON.parseObject(JSON.toJSONString(document));
			com.alibaba.fastjson2.JSONArray segments = new com.alibaba.fastjson2.JSONArray();
			for (KbSegment segment : segmentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KbSegment>()
					.eq("document_id", document.getId()).orderByAsc("position"))) {
				segments.add(JSON.parseObject(JSON.toJSONString(segment)));
			}
			docJson.put("segments", segments);
			documents.add(docJson);
		}
		payload.put("documents", documents);
		return payload;
	}

	/**
	 * 导入数据集：按导出 JSON 重建数据集/文档/分段（数据集与文档 id 重新生成，分段保留内容与
	 * keywords/answer），全部文档置 waiting 入队重新嵌入。indexing_status 重置为 waiting。
	 *
	 * @return 新数据集 id
	 */
	public String importDataset(com.alibaba.fastjson2.JSONObject payload) {
		state.checkReady();
		if (payload == null || payload.getJSONObject("dataset") == null) {
			throw new IllegalArgumentException("导入内容缺少 dataset 配置");
		}
		KbDataset dataset = payload.getJSONObject("dataset").toJavaObject(KbDataset.class);
		String sourceName = dataset.getName();
		dataset.setId(UUID.randomUUID().toString().replace("-", ""));
		dataset.setName(sourceName == null ? "导入数据集" : sourceName + "-导入");
		// 重新嵌入后回填，导入时清空运行态字段
		dataset.setEmbeddingModelActive(null);
		dataset.setErrorMsg(null);
		Date now = new Date();
		dataset.setCreateTime(now);
		dataset.setUpdateTime(now);
		dataset.setDelFlag("0");
		validate(dataset);
		datasetMapper.insert(dataset);

		com.alibaba.fastjson2.JSONArray documents = payload.getJSONArray("documents");
		if (documents != null) {
			for (int i = 0; i < documents.size(); i++) {
				com.alibaba.fastjson2.JSONObject docJson = documents.getJSONObject(i);
				KbDocument document = docJson.toJavaObject(KbDocument.class);
				String sourceDocumentId = document.getId();
				document.setId(UUID.randomUUID().toString().replace("-", ""));
				document.setDatasetId(dataset.getId());
				document.setIndexingStatus("waiting");
				document.setLatencyMs(null);
				document.setErrorMsg(null);
				document.setCreateTime(now);
				document.setUpdateTime(now);
				documentMapper.insert(document);
				com.alibaba.fastjson2.JSONArray segments = docJson.getJSONArray("segments");
				if (segments == null) {
					continue;
				}
				for (int j = 0; j < segments.size(); j++) {
					com.alibaba.fastjson2.JSONObject segmentJson = segments.getJSONObject(j);
					segmentJson.put("id", java.util.UUID.randomUUID().toString().replace("-", ""));
					segmentJson.put("datasetId", dataset.getId());
					segmentJson.put("documentId", document.getId());
					segmentJson.put("status", "waiting");
					segmentJson.put("indexNodeId", null);
					segmentJson.put("indexNodeHash", null);
					segmentJson.put("hitCount", 0);
					segmentJson.put("createTime", now.getTime());
					segmentJson.put("updateTime", now.getTime());
					KbSegment segment = segmentJson.toJavaObject(KbSegment.class);
					segment.setCreateTime(now);
					segment.setUpdateTime(now);
					segmentMapper.insert(segment);
				}
				// 重新入队嵌入（导入的向量不在导出内容内）
				indexingService.trigger(document);
			}
		}
		return dataset.getId();
	}
}
