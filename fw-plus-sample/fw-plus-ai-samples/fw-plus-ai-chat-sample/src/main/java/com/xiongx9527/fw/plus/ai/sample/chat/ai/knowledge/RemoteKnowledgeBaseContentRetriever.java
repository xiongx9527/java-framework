package com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 调用远程知识库接口的 {@code ContentRetriever} 实现。
 *
 * <p>这是本样例的核心：它把"自有知识库"接进 LangChain4j 的 RAG 链路。
 * LangChain4j 只提供 {@link dev.langchain4j.rag.content.retriever.ContentRetriever} 接口，
 * 检索逻辑全部由实现方决定——这里用 HTTP 调用远程服务，本地不持有任何向量数据。</p>
 *
 * <p>降级策略：远程不可达、超时或返回空时一律返回空列表，不抛出异常，
 * 让对话在没有知识库上下文的情况下继续（对应验收 A6）。</p>
 */
public class RemoteKnowledgeBaseContentRetriever implements dev.langchain4j.rag.content.retriever.ContentRetriever {

	private static final Logger log = LoggerFactory.getLogger(RemoteKnowledgeBaseContentRetriever.class);

	private final RestTemplate restTemplate;
	private final KnowledgeBaseProperties properties;

	public RemoteKnowledgeBaseContentRetriever(RestTemplate restTemplate, KnowledgeBaseProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	/**
	 * 静态工厂：按配置创建带超时的 {@link RestTemplate} 与检索器实例。
	 */
	public static RemoteKnowledgeBaseContentRetriever of(KnowledgeBaseProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(properties.getConnectTimeoutMillis());
		factory.setReadTimeout(properties.getReadTimeoutMillis());
		return new RemoteKnowledgeBaseContentRetriever(new RestTemplate(factory), properties);
	}

	@Override
	public List<Content> retrieve(Query query) {
		String text = query.text();
		if (text == null || text.trim().isEmpty()) {
			return Collections.emptyList();
		}

		KnowledgeSearchRequest request = new KnowledgeSearchRequest(text, properties.getTopK());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<KnowledgeSearchRequest> entity = new HttpEntity<>(request, headers);

		try {
			String url = properties.getBaseUrl() + properties.getSearchPath();
			log.debug("调用知识库接口: url={}, query={}, topK={}", url, text, properties.getTopK());

			ResponseEntity<KnowledgeSearchResponse> response = restTemplate.postForEntity(
					url, entity, KnowledgeSearchResponse.class);

			KnowledgeSearchResponse body = response.getBody();
			if (body == null || body.getItems() == null || body.getItems().isEmpty()) {
				log.debug("知识库未返回匹配条目: query={}", text);
				return Collections.emptyList();
			}

			List<Content> contents = new ArrayList<>();
			for (KnowledgeSearchResponse.Item item : body.getItems()) {
				if (item.getContent() == null || item.getContent().trim().isEmpty()) {
					continue;
				}
				TextSegment segment = item.getTitle() == null
						? TextSegment.from(item.getContent())
						: TextSegment.from(item.getContent(), metadataOf("title", item.getTitle()));
				contents.add(Content.from(segment));
			}
			log.debug("知识库返回 {} 条上下文: query={}", contents.size(), text);
			return contents;
		} catch (Exception e) {
			// 知识库故障不应打断对话：记录后按"无上下文"处理（A6）
			log.warn("知识库接口调用失败，本次按无知识库上下文继续: query={}, error={}", text, e.toString());
			return Collections.emptyList();
		}
	}

	/** 构造只含一个键值的 Metadata（0.35 没有 Metadata.from(String,Object)）。 */
	private static dev.langchain4j.data.document.Metadata metadataOf(String key, String value) {
		dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
		metadata.put(key, value);
		return metadata;
	}

	/** 知识库检索请求体：{@code {"query":"...","topK":3}}。 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class KnowledgeSearchRequest {
		private final String query;
		private final int topK;

		public KnowledgeSearchRequest(String query, int topK) {
			this.query = query;
			this.topK = topK;
		}

		public String getQuery() {
			return query;
		}

		public int getTopK() {
			return topK;
		}
	}

	/** 知识库检索响应体：{@code {"items":[{"title":"...","content":"...","score":0.87}]}}。 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class KnowledgeSearchResponse {
		private List<Item> items;

		public List<Item> getItems() {
			return items;
		}

		public void setItems(List<Item> items) {
			this.items = items;
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Item {
			private String title;
			private String content;
			private Double score;

			public String getTitle() {
				return title;
			}

			public void setTitle(String title) {
				this.title = title;
			}

			public String getContent() {
				return content;
			}

			public void setContent(String content) {
				this.content = content;
			}

			public Double getScore() {
				return score;
			}

			public void setScore(Double score) {
				this.score = score;
			}
		}
	}
}
