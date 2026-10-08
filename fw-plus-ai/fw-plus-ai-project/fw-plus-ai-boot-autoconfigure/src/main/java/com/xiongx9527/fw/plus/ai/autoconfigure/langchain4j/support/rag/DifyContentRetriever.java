package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag;

import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.dataset.entity.Document;
import com.xiongx9527.fw.plus.dify.dataset.entity.Segment;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrieveResult;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrievalModel;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeMetadataCondition;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 dify 知识库检索接口的 {@link ContentRetriever} 实现（fw-plus-dify SDK）。
 *
 * <p>执行规则：</p>
 * <ul>
 *   <li><b>并行扇出</b>：多知识库同时调用 {@code IDatasetClient#retrieve}
 *       （dify {@code POST /datasets/{dataset_id}/retrieve}），总耗时约等于最慢的一路；
 *       单库失败只记 WARN 并跳过该库（多库检索中一路不可用不应让整次检索失败）；</li>
 *   <li><b>元数据过滤</b>：开关开启时把会话 {@code chatMemoryId} 四段转成
 *       {@code retrieval_model.metadata_filtering_conditions}（参与段与字段名可配，
 *       见 {@link DifyRagProperties.MetaFields}），非四段契约则不过滤；</li>
 *   <li>多库结果按 {@code score} 降序合并（无分数排最后），截断到 {@code topK} 条；</li>
 *   <li>正文取 {@code segment.content}，metadata 携带 {@code title}（文档名）、{@code score}、
 *       {@code datasetId}、{@code segmentId}、{@code answer}（Q&amp;A 分段，非空才有）、
 *       {@code keywords}（逗号拼接）；</li>
 *   <li>检索异常最终降级返回空列表，不打断对话链路。</li>
 * </ul>
 *
 * <p>线程池按配置创建（{@code pool.size <=0} 时按知识库数量），{@link #close()} 时释放
 * （作为 Spring Bean 时由容器自动关闭）。</p>
 */
public class DifyContentRetriever implements ContentRetriever, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(DifyContentRetriever.class);

	private final IDatasetClient client;

	private final List<String> datasetIds;

	/** 基础检索参数（无过滤条件时复用，只读不改） */
	private final RetrievalModel baseModel;

	/** 多库合并后的全局截断上限（&lt;=0 表示不限制） */
	private final int topK;

	private final DifyRagProperties.MetaFields metaFields;

	/** 并行检索执行池；单库或已关闭时为 null */
	private ExecutorService executor;

	public DifyContentRetriever(IDatasetClient client, DifyRagProperties properties) {
		this.client = client;
		this.datasetIds = normalize(properties.getDatasetIds());
		this.baseModel = properties.toRetrievalModel();
		this.topK = properties.getTopK();
		this.metaFields = properties.getMetaFields();
		this.executor = this.datasetIds.size() > 1
				? newExecutor(properties.getPool(), this.datasetIds.size()) : null;
	}

	@Override
	public List<Content> retrieve(Query query) {
		if (query == null || datasetIds.isEmpty()) {
			return Collections.emptyList();
		}
		String text = query.text();
		if (text == null || text.trim().isEmpty()) {
			return Collections.emptyList();
		}
		try {
			List<Candidate> candidates = fetchCandidates(query, text);
			// 分数降序合并，无分数（null）排最后
			candidates.sort(Comparator
					.comparingDouble((Candidate c) -> c.score == null ? Double.NEGATIVE_INFINITY : c.score)
					.reversed());
			List<Content> contents = new ArrayList<>(candidates.size());
			for (int i = 0; i < candidates.size(); i++) {
				if (topK > 0 && i >= topK) {
					break;
				}
				contents.add(toContent(candidates.get(i)));
			}
			return contents;
		} catch (Exception e) {
			// 知识库故障不应打断对话：按「无上下文」降级
			logger.warn("Dify 知识库检索失败，本次按无知识库上下文继续: query={}, datasets={}, error={}",
					text, datasetIds, e.toString());
			return Collections.emptyList();
		}
	}

	/** 单库同步直查；多库并行扇出、单库失败跳过（一路不可用不影响其余库） */
	private List<Candidate> fetchCandidates(Query query, String text) {
		RetrievalModel model = resolveModel(query);
		if (datasetIds.size() == 1) {
			return fetch(datasetIds.get(0), text, model);
		}
		List<Future<List<Candidate>>> futures = new ArrayList<>(datasetIds.size());
		for (String datasetId : datasetIds) {
			final String id = datasetId;
			futures.add(executor.submit(() -> fetch(id, text, model)));
		}
		List<Candidate> merged = new ArrayList<>();
		for (int index = 0; index < futures.size(); index++) {
			try {
				List<Candidate> part = futures.get(index).get();
				if (part != null) {
					merged.addAll(part);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return merged;
			} catch (ExecutionException e) {
				logger.warn("Dify 知识库检索失败，该库本次跳过: dataset={}, error={}",
						datasetIds.get(index), String.valueOf(e.getCause()));
			}
		}
		return merged;
	}

	/** 单库召回：解析服务端结果为候选（跳过空正文/缺分段的脏数据） */
	private List<Candidate> fetch(String datasetId, String text, RetrievalModel model) {
		RetrieveResult result = client.retrieve(datasetId, text, model);
		List<Candidate> candidates = new ArrayList<>();
		if (result == null || result.getRecords() == null) {
			return candidates;
		}
		for (RetrieveResult.Record record : result.getRecords()) {
			if (record == null || record.getSegment() == null) {
				continue;
			}
			Segment segment = record.getSegment();
			if (segment.getContent() == null || segment.getContent().trim().isEmpty()) {
				continue;
			}
			candidates.add(new Candidate(datasetId, record.getScore(), segment));
		}
		return candidates;
	}

	/**
	 * 解析本次检索的 RetrievalModel：开关开启且 chatMemoryId 为四段契约时，
	 * 按 include/字段名配置生成过滤条件并复制一份基础参数（共享 baseModel 不可变）。
	 */
	private RetrievalModel resolveModel(Query query) {
		KnowledgeMetadataCondition condition = buildCondition(query);
		if (condition == null) {
			return baseModel;
		}
		return RetrievalModel.builder()
				.searchMethod(baseModel.getSearchMethod())
				.rerankingEnable(baseModel.getRerankingEnable())
				.rerankingModel(baseModel.getRerankingModel())
				.weights(baseModel.getWeights())
				.topK(baseModel.getTopK())
				.scoreThresholdEnabled(baseModel.getScoreThresholdEnabled())
				.scoreThreshold(baseModel.getScoreThreshold())
				.metadataFilteringConditions(condition)
				.build();
	}

	/** chatMemoryId 四段 → metadata_filtering_conditions（AND）；条件不满足任何一条返回 null */
	private KnowledgeMetadataCondition buildCondition(Query query) {
		if (metaFields == null || !metaFields.isEnabled()) {
			return null;
		}
		dev.langchain4j.rag.query.Metadata metadata = query.metadata();
		if (metadata == null || metadata.chatMemoryId() == null) {
			return null;
		}
		String memoryId = metadata.chatMemoryId().toString();
		String[] segments = memoryId.split(":", -1);
		if (segments.length != 4) {
			logger.debug("chatMemoryId 非四段契约，本次检索不做元数据过滤: memoryId={}", memoryId);
			return null;
		}
		List<KnowledgeMetadataCondition.Condition> conditions = new ArrayList<>();
		addCondition(conditions, "tenant", metaFields.getTenantField(), segments[0]);
		addCondition(conditions, "app", metaFields.getAppField(), segments[1]);
		addCondition(conditions, "agent", metaFields.getAgentField(), segments[2]);
		addCondition(conditions, "user", metaFields.getUserField(), segments[3]);
		if (conditions.isEmpty()) {
			return null;
		}
		KnowledgeMetadataCondition condition = new KnowledgeMetadataCondition();
		condition.setLogical_operator("and");
		condition.setConditions(conditions);
		return condition;
	}

	/** 段参与（include 命中）且字段名非空才生成单条条件；operator=is 精确匹配 */
	private void addCondition(List<KnowledgeMetadataCondition.Condition> conditions,
			String segmentName, String fieldName, String value) {
		List<String> include = metaFields.getInclude();
		if (include != null && !containsSegment(include, segmentName)) {
			return;
		}
		if (fieldName == null || fieldName.trim().isEmpty()) {
			return;
		}
		KnowledgeMetadataCondition.Condition condition = new KnowledgeMetadataCondition.Condition();
		condition.setName(fieldName.trim());
		condition.setComparison_operator("is");
		condition.setValue(value);
		conditions.add(condition);
	}

	private static boolean containsSegment(List<String> include, String segmentName) {
		for (String item : include) {
			if (item != null && segmentName.equals(item.trim())) {
				return true;
			}
		}
		return false;
	}

	private static Content toContent(Candidate candidate) {
		Segment segment = candidate.segment;
		Metadata metadata = new Metadata();
		if (candidate.score != null) {
			metadata.put("score", candidate.score.toString());
		}
		metadata.put("datasetId", candidate.datasetId);
		if (segment.getId() != null) {
			metadata.put("segmentId", segment.getId());
		}
		Document document = segment.getDocument();
		if (document != null && document.getName() != null) {
			metadata.put("title", document.getName());
		}
		String answer = segment.getAnswer();
		if (answer != null && !answer.trim().isEmpty()) {
			metadata.put("answer", answer);
		}
		List<String> keywords = segment.getKeywords();
		if (keywords != null && !keywords.isEmpty()) {
			metadata.put("keywords", String.join(",", keywords));
		}
		return Content.from(TextSegment.from(segment.getContent(), metadata));
	}

	private static List<String> normalize(List<String> datasetIds) {
		if (datasetIds == null || datasetIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> normalized = new ArrayList<>(datasetIds.size());
		for (String datasetId : datasetIds) {
			if (datasetId != null && !datasetId.trim().isEmpty()) {
				normalized.add(datasetId.trim());
			}
		}
		return Collections.unmodifiableList(normalized);
	}

	@Override
	public void close() {
		ExecutorService current = executor;
		executor = null;
		if (current != null) {
			current.shutdownNow();
		}
	}

	/** 按配置建池：pool.size <=0 时按知识库数量，配小可限并发（任务自动排队） */
	private static ExecutorService newExecutor(DifyRagProperties.Pool pool, int datasetCount) {
		int size = pool != null && pool.getSize() > 0 ? pool.getSize() : datasetCount;
		String prefix = pool == null || pool.getThreadNamePrefix() == null || pool.getThreadNamePrefix().trim().isEmpty()
				? "dify-content-retriever" : pool.getThreadNamePrefix().trim();
		final String threadPrefix = prefix;
		final AtomicInteger counter = new AtomicInteger();
		ThreadFactory factory = runnable -> {
			Thread thread = new Thread(runnable, threadPrefix + "-" + counter.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		};
		return Executors.newFixedThreadPool(size, factory);
	}

	/** 单条召回候选：库 ID + 分数 + 分段原文 */
	private static final class Candidate {
		private final String datasetId;
		private final Double score;
		private final Segment segment;

		private Candidate(String datasetId, Double score, Segment segment) {
			this.datasetId = datasetId;
			this.score = score;
			this.segment = segment;
		}
	}
}
