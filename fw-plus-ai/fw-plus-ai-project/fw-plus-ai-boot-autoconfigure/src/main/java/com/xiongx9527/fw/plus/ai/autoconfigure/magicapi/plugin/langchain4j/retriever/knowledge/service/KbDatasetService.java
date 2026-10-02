package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
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

	private final KbSearchService searchService;

	private final KbIndexingService indexingService;

	private final ObjectProvider<MagicResourceService> resourceServices;

	private final ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.resource.Langchain4jContentRetrieverMagicResourceStorage> retrieverStorages;

	private final KbKnowledgeState state;

	public KbDatasetService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSearchService searchService, KbIndexingService indexingService,
			ObjectProvider<MagicResourceService> resourceServices,
			ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.resource.Langchain4jContentRetrieverMagicResourceStorage> retrieverStorages,
			KbKnowledgeState state) {
		this.datasetMapper = datasetMapper;
		this.documentMapper = documentMapper;
		this.searchService = searchService;
		this.indexingService = indexingService;
		this.resourceServices = resourceServices;
		this.retrieverStorages = retrieverStorages;
		this.state = state;
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
		validate(dataset);
		if (dataset.getId() == null || dataset.getId().isEmpty()) {
			dataset.setId(UUID.randomUUID().toString().replace("-", ""));
		}
		if (dataset.getIndexingTechnique() == null || dataset.getIndexingTechnique().isEmpty()) {
			dataset.setIndexingTechnique("high_quality");
		}
		if (dataset.getRetrievalTopK() == null) {
			dataset.setRetrievalTopK(4);
		}
		if (dataset.getRetrievalScoreThreshold() == null) {
			dataset.setRetrievalScoreThreshold(new java.math.BigDecimal("0.0000"));
		}
		if (dataset.getSegmentMaxLength() == null) {
			dataset.setSegmentMaxLength(1000);
		}
		if (dataset.getSegmentOverlap() == null) {
			dataset.setSegmentOverlap(200);
		}
		if (dataset.getSegmentMode() == null || dataset.getSegmentMode().isEmpty()) {
			dataset.setSegmentMode("general");
		}
		if (dataset.getStatus() == null || dataset.getStatus().isEmpty()) {
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

	/** 数据集下的文档列表（管理端文档视图）。 */
	public List<KbDocument> listDocuments(String datasetId) {
		state.checkReady();
		requireDataset(datasetId);
		return documentMapper.selectList(new QueryWrapper<KbDocument>()
				.eq("dataset_id", datasetId)
				.orderByDesc("create_time"));
	}

	/** 召回测试（设计 §3.1）：query → 嵌入 → 向量检索 → 过滤 → rerank → 命中分段与 score。 */
	public List<KbSearchHit> hitTest(String id, String query, Integer topK, Double minScore,
			JSONObject metadataCondition) {
		state.checkReady();
		requireDataset(id);
		return searchService.search(id, query, topK, minScore, metadataCondition);
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

	private void validate(KbDataset dataset) {
		if (dataset.getName() == null || dataset.getName().isEmpty()) {
			throw new IllegalArgumentException("数据集名称不能为空");
		}
		if (dataset.getEmbeddingModelKey() == null || dataset.getEmbeddingModelKey().isEmpty()) {
			throw new IllegalArgumentException("嵌入模型资源 key（embedding-model-key）不能为空");
		}
		if (dataset.getEmbeddingStoreKey() == null || dataset.getEmbeddingStoreKey().isEmpty()) {
			throw new IllegalArgumentException("向量库资源 key（embedding-store-key）不能为空");
		}
		String mode = dataset.getSegmentMode();
		if (mode != null && !mode.isEmpty() && !"general".equals(mode) && !"parent-child".equals(mode)
				&& !"qa".equals(mode)) {
			throw new IllegalArgumentException("不支持的分段模式: " + mode + "（general / parent-child / qa）");
		}
	}

	private KbDataset requireDataset(String id) {
		KbDataset dataset = datasetMapper.selectById(id);
		if (dataset == null) {
			throw new IllegalArgumentException("知识库数据集不存在: " + id);
		}
		return dataset;
	}
}
