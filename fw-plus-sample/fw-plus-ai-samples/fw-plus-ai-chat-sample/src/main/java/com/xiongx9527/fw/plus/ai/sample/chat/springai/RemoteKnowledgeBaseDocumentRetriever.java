package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.KnowledgeBaseProperties;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.KnowledgeSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程知识库 → Spring AI 原生 {@link DocumentRetriever} 适配器。
 *
 * <p>HTTP 检索细节（URL、请求体、超时、故障降级为无上下文）与 LangChain4j 链路
 * 共用同一个 {@link KnowledgeSearchClient}，只在此做结果映射
 * （Item → Document，title/score 进 metadata）与 min-score 阈值过滤
 * （阈值语义与框架 {@code FilteringContentRetriever} 一致）。</p>
 *
 * <p>装配见 {@link SpringAiChatConfig}：命中知识库配置时，两条 Spring AI 链路的
 * ChatClient 都会挂 {@code RetrievalAugmentationAdvisor}，由它在每轮对话前调用本
 * 检索器，检索结果经官方 {@code ContextualQueryAugmenter} 注入本轮 user message
 * （不写入会话记忆），并随 {@code ChatResponse.metadata} 的 {@code rag_document_context}
 * 透出；未配置 {@code base-url} 时 Bean 不装配、Advisor 不挂，链路退化为纯对话。</p>
 */
public class RemoteKnowledgeBaseDocumentRetriever implements DocumentRetriever {

	/** 注入文档文本的日志截断长度（DEBUG 明细用，避免整段上下文刷屏）。 */
	private static final int LOG_TEXT_LIMIT = 80;

	private static final Logger log = LoggerFactory.getLogger(RemoteKnowledgeBaseDocumentRetriever.class);

	private final KnowledgeSearchClient searchClient;
	private final KnowledgeBaseProperties properties;

	public RemoteKnowledgeBaseDocumentRetriever(KnowledgeSearchClient searchClient,
			KnowledgeBaseProperties properties) {
		this.searchClient = searchClient;
		this.properties = properties;
	}

	/**
	 * 静态工厂：按配置创建带超时的客户端实例。
	 */
	public static RemoteKnowledgeBaseDocumentRetriever of(KnowledgeBaseProperties properties) {
		return new RemoteKnowledgeBaseDocumentRetriever(KnowledgeSearchClient.of(properties), properties);
	}

	@Override
	public List<Document> retrieve(Query query) {
		if (query == null || query.text() == null || query.text().trim().isEmpty()) {
			return Collections.emptyList();
		}

		// 与 LangChain4j 链路 RemoteKnowledgeBaseContentRetriever 同款起点日志
		log.info("=======================>检索知识库上下文: {}", query.text());

		List<KnowledgeSearchClient.KnowledgeSearchResponse.Item> items = searchClient.search(query.text());
		if (items == null || items.isEmpty()) {
			return Collections.emptyList();
		}

		List<Document> documents = new ArrayList<>();
		int droppedByScore = 0;
		for (KnowledgeSearchClient.KnowledgeSearchResponse.Item item : items) {
			if (item.getContent() == null || item.getContent().trim().isEmpty()) {
				continue;
			}
			Double score = item.getScore();
			if (score != null && score < properties.getMinScore()) {
				// 低于阈值的兜底条目不进 prompt
				droppedByScore++;
				continue;
			}
			Map<String, Object> metadata = new LinkedHashMap<>();
			if (item.getTitle() != null && !item.getTitle().trim().isEmpty()) {
				metadata.put("title", item.getTitle().trim());
			}
			if (score != null) {
				metadata.put("score", score);
			}
			documents.add(new Document(item.getContent().trim(), metadata));
			// 明细到 DEBUG：Spring AI 侧没有 ChatContextLogger，注入内容靠这里排查
			log.debug("注入知识库文档[{}]: title={}, score={}, text={}",
					documents.size(), metadata.get("title"), score, truncate(item.getContent().trim()));
		}
		log.debug("知识库返回 {} 条上下文，min-score={} 过滤 {} 条，最终注入 {} 条: query={}",
				items.size(), properties.getMinScore(), droppedByScore, documents.size(), query.text());
		return documents;
	}

	/** 日志用文本截断：超过 {@link #LOG_TEXT_LIMIT} 字符时截断并追加省略号。 */
	private static String truncate(String text) {
		return text.length() <= LOG_TEXT_LIMIT ? text : text.substring(0, LOG_TEXT_LIMIT) + "...";
	}
}
