package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchHit;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * knowledge 类型检索实例（设计 §3.2）：懒解析——检索时才按 dataset-id 解析数据集配置与嵌入/向量资源，
 * 注册顺序与数据集创建先后不影响构建。过滤链（enabled/archived/score 阈值）与 hit_count 累加
 * 统一由 {@link KbSearchService#search} 承担。
 */
public class KnowledgeContentRetriever implements ContentRetriever,
		com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant.FilterableRetriever {

	private final ObjectProvider<KbSearchService> searchServices;

	/** 数据集配置的对话模型（auto 模式生成过滤条件用；经 ContentRetrieverContext 懒解析） */
	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext retrieverContext;

	private final String datasetId;

	private final Integer maxResults;

	private final Double minScore;

	/** 固定文档限定（选填） */
	private final String documentId;

	/** 固定元数据过滤条件（选填，Dify metadata_condition 格式） */
	private final com.alibaba.fastjson2.JSONObject metadataCondition;

	/**
	 * 元数据过滤模式：none（默认，仅用固定 metadataCondition）/
	 * auto（检索前由 LLM 从用户消息与数据集字段定义自动生成过滤条件，与固定条件叠加）。
	 */
	private final String metadataFilterMode;

	/** 字段定义服务（auto 模式拉取字段定义构造提取 prompt） */
	private final ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService> metadataFieldServices;


	public KnowledgeContentRetriever(ObjectProvider<KbSearchService> searchServices, String datasetId,
			Integer maxResults, Double minScore) {
		this(searchServices, datasetId, maxResults, minScore, null, null, "none", null, null);
	}

	public KnowledgeContentRetriever(ObjectProvider<KbSearchService> searchServices, String datasetId,
			Integer maxResults, Double minScore, String documentId,
			com.alibaba.fastjson2.JSONObject metadataCondition) {
		this(searchServices, datasetId, maxResults, minScore, documentId, metadataCondition, "none", null, null);
	}

	public KnowledgeContentRetriever(ObjectProvider<KbSearchService> searchServices, String datasetId,
			Integer maxResults, Double minScore, String documentId,
			com.alibaba.fastjson2.JSONObject metadataCondition, String metadataFilterMode,
			ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService> metadataFieldServices,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext retrieverContext) {
		this.searchServices = searchServices;
		this.datasetId = datasetId;
		this.maxResults = maxResults;
		this.minScore = minScore;
		this.documentId = documentId;
		this.metadataCondition = metadataCondition;
		this.metadataFilterMode = metadataFilterMode == null ? "none" : metadataFilterMode;
		this.metadataFieldServices = metadataFieldServices;
		this.retrieverContext = retrieverContext;
	}

	@Override
	public List<Content> retrieve(Query query) {
		return doRetrieve(query.text(), metadataCondition);
	}

	/**
	 * 显式带元数据过滤的检索：{@code extraCondition}（Dify metadata_condition 格式，可 null）
	 * 与构建期固定 {@code metadataCondition} 叠加（AND），供 SimpleAssistant 的过滤重载
	 * 经回调直达，不依赖 ThreadLocal 隐式传参。
	 */
	public List<Content> retrieve(String message, com.alibaba.fastjson2.JSONObject extraCondition) {
		return doRetrieve(message, mergeConditions(metadataCondition, extraCondition));
	}

	private List<Content> doRetrieve(String queryText, com.alibaba.fastjson2.JSONObject baseCondition) {
		KbSearchService searchService = searchServices == null ? null : searchServices.getIfAvailable();
		if (searchService == null) {
			throw new IllegalStateException("知识库检索服务未装配，无法构建 knowledge 类型检索器");
		}
		com.alibaba.fastjson2.JSONObject effectiveCondition = baseCondition;
		// auto 模式：由 LLM 从用户消息与字段定义自动生成元数据过滤条件（失败回落无条件检索）
		if ("auto".equalsIgnoreCase(metadataFilterMode) && metadataFieldServices != null) {
			com.alibaba.fastjson2.JSONObject generated = generateConditionByLlm(queryText);
			if (generated != null && !generated.isEmpty()) {
				effectiveCondition = mergeConditions(effectiveCondition, generated);
			}
		}
		List<KbSearchHit> hits = searchService.search(datasetId, queryText, maxResults, minScore,
				effectiveCondition, documentId);
		List<Content> contents = new ArrayList<>(hits.size());
		for (KbSearchHit hit : hits) {
			Metadata metadata = new Metadata();
			metadata.put("dataset_id", hit.getDatasetId());
			metadata.put("document_id", hit.getDocumentId());
			metadata.put("segment_id", hit.getSegmentId());
			if (hit.getDocumentName() != null) {
				metadata.put("document_name", hit.getDocumentName());
			}
			if (hit.getPosition() > 0) {
				metadata.put("position", String.valueOf(hit.getPosition()));
			}
			if (hit.getMetadata() != null) {
				for (Map.Entry<String, Object> entry : hit.getMetadata().entrySet()) {
					if (entry.getKey() != null && entry.getValue() != null) {
						metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
					}
				}
			}
			contents.add(Content.from(TextSegment.from(hit.getContent(), metadata)));
		}
		return contents;
	}

	/**
	 * auto 模式：由数据集配置的对话模型从用户消息与字段定义生成 metadata_condition。
	 * 字段定义未配置/对话模型不可用/输出不合法时返回 null（回落无条件检索）。
	 */
	private com.alibaba.fastjson2.JSONObject generateConditionByLlm(String userMessage) {
		try {
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService fieldService =
					metadataFieldServices == null ? null : metadataFieldServices.getIfAvailable();
			if (fieldService == null || retrieverContext == null) {
				return null;
			}
			List<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbMetadataField> fields =
					fieldService.list(datasetId);
			if (fields.isEmpty()) {
				return null;
			}
			// 对话模型：取 Spring 默认实例（chat model 数据源模块的默认链路）
			dev.langchain4j.model.chat.ChatLanguageModel chatModel = retrieverContext.chatModel(null);
			if (chatModel == null) {
				return null;
			}
			StringBuilder fieldDefs = new StringBuilder();
			for (com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbMetadataField field : fields) {
				fieldDefs.append("- ").append(field.getName()).append("（").append(field.getType()).append("）");
				if (field.getDescription() != null && !field.getDescription().isEmpty()) {
					fieldDefs.append("：").append(field.getDescription());
				}
				fieldDefs.append('\n');
			}
			String prompt = "请从用户消息中提取可用于文档元数据过滤的条件。\n"
					+ "可用字段定义（名称、类型、说明）：\n" + fieldDefs
					+ "\n规则：\n"
					+ "1. 只使用上面列出的字段，未提及的不要编造；\n"
					+ "2. comparison_operator 仅可用：is / is not / in / not in / gt / gte / lt / lte / contains / is empty / is not empty；\n"
					+ "3. 用户消息没有可用于过滤的信息时，输出空对象 {}；\n"
					+ "4. 只输出 JSON，格式：{\"logical_operator\":\"and\",\"conditions\":[{\"name\":\"字段名\",\"comparison_operator\":\"is\",\"value\":\"值\"}]}。\n"
					+ "\n用户消息：" + userMessage;
			String response = chatModel.generate(prompt);
			if (response == null || response.isEmpty()) {
				return null;
			}
			String json = stripCodeFence(response);
			int start = json.indexOf('{');
			int end = json.lastIndexOf('}');
			if (start < 0 || end <= start) {
				return null;
			}
			com.alibaba.fastjson2.JSONObject parsed = com.alibaba.fastjson2.JSON.parseObject(
					json.substring(start, end + 1));
			return parsed != null && parsed.containsKey("conditions") ? parsed : null;
		} catch (Exception e) {
			// 生成失败不阻断检索
			return null;
		}
	}

	private static String stripCodeFence(String response) {
		String trimmed = response.trim();
		if (trimmed.startsWith("```")) {
			int first = trimmed.indexOf('\n');
			int last = trimmed.lastIndexOf("```");
			if (first > 0 && last > first) {
				return trimmed.substring(first + 1, last);
			}
		}
		return trimmed;
	}

	/** 条件叠加（AND）：任一入参可为 null。 */
	private static com.alibaba.fastjson2.JSONObject mergeConditions(
			com.alibaba.fastjson2.JSONObject first, com.alibaba.fastjson2.JSONObject second) {
		if (first == null || first.isEmpty()) {
			return second;
		}
		if (second == null || second.isEmpty()) {
			return first;
		}
		com.alibaba.fastjson2.JSONObject merged = new com.alibaba.fastjson2.JSONObject();
		merged.put("logical_operator", "and");
		com.alibaba.fastjson2.JSONArray conditions = new com.alibaba.fastjson2.JSONArray();
		if (first.getJSONArray("conditions") != null) {
			for (Object item : first.getJSONArray("conditions")) {
				conditions.add(item);
			}
		}
		if (second.getJSONArray("conditions") != null) {
			for (Object item : second.getJSONArray("conditions")) {
				conditions.add(item);
			}
		}
		merged.put("conditions", conditions);
		return merged;
	}
}
