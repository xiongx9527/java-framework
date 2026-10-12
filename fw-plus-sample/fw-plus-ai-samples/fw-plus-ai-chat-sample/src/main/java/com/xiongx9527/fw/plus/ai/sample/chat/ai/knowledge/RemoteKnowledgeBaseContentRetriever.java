package com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 调用远程知识库接口的 {@code ContentRetriever} 实现。
 *
 * <p>这是本样例的核心：它把"自有知识库"接进 LangChain4j 的 RAG 链路。
 * LangChain4j 只提供 {@link dev.langchain4j.rag.content.retriever.ContentRetriever} 接口，
 * 检索逻辑全部由实现方决定——这里用 HTTP 调用远程服务，本地不持有任何向量数据。</p>
 *
 * <p>HTTP 检索逻辑委托给框架中立的 {@link KnowledgeSearchClient}（LangChain4j 与
 * Spring AI 两套链路共用一份实现），本类只负责把 DTO 映射成 LangChain4j 的
 * {@link Content}；降级策略见客户端：远程不可达、超时或返回空时一律返回空列表，
 * 不抛出异常，让对话在没有知识库上下文的情况下继续（对应验收 A6）。</p>
 */
public class RemoteKnowledgeBaseContentRetriever implements dev.langchain4j.rag.content.retriever.ContentRetriever {

	private static final Logger log = LoggerFactory.getLogger(RemoteKnowledgeBaseContentRetriever.class);

	private final KnowledgeSearchClient searchClient;

	public RemoteKnowledgeBaseContentRetriever(RestTemplate restTemplate, KnowledgeBaseProperties properties) {
		this.searchClient = new KnowledgeSearchClient(restTemplate, properties);
	}

	/**
	 * 静态工厂：按配置创建带超时的 {@link RestTemplate} 与检索器实例。
	 */
	public static RemoteKnowledgeBaseContentRetriever of(KnowledgeBaseProperties properties) {
		return new RemoteKnowledgeBaseContentRetriever(
				KnowledgeSearchClient.newRestTemplate(properties), properties);
	}

	@Override
	public List<Content> retrieve(Query query) {
		log.info("=======================>检索知识库上下文: {}", query.text());
		List<KnowledgeSearchClient.KnowledgeSearchResponse.Item> items = searchClient.search(query.text());
		if (items.isEmpty()) {
			return Collections.emptyList();
		}

		List<Content> contents = new ArrayList<>();
		for (KnowledgeSearchClient.KnowledgeSearchResponse.Item item : items) {
			if (item.getContent() == null || item.getContent().trim().isEmpty()) {
				continue;
			}
			dev.langchain4j.data.document.Metadata metadata =
					new dev.langchain4j.data.document.Metadata();
			Map<String, Object> fromServer = item.getMetadata();
			if (fromServer != null) {
				for (Map.Entry<String, Object> entry : fromServer.entrySet()) {
					if (entry.getValue() != null) {
						metadata.put(entry.getKey(), entry.getValue().toString());
					}
				}
			}
			if (!metadata.containsKey("title") && item.getTitle() != null) {
				metadata.put("title", item.getTitle());
			}
			if (!metadata.containsKey("score") && item.getScore() != null) {
				// score 也要带下来：FilteringContentRetriever 要按阈值过滤
				metadata.put("score", item.getScore().toString());
			}
			TextSegment segment = metadata.toMap().isEmpty()
					? TextSegment.from(item.getContent())
					: TextSegment.from(item.getContent(), metadata);
			contents.add(Content.from(segment));
		}
		log.debug("知识库返回 {} 条上下文: query={}", contents.size(), query.text());
		return contents;
	}
}
