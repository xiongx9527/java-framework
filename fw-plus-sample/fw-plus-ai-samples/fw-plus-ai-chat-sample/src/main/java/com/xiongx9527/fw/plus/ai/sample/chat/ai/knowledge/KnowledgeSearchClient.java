package com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 远程知识库检索的框架中立客户端：只发 HTTP、只回 DTO，不依赖 LangChain4j 或 Spring AI 类型。
 *
 * <p>LangChain4j 侧由 {@link RemoteKnowledgeBaseContentRetriever} 把结果映射成 {@code Content}；
 * Spring AI 侧由 {@code springai/RemoteKnowledgeBaseDocumentRetriever} 映射成 {@code Document}
 * 并接进 {@code RetrievalAugmentationAdvisor}。检索逻辑（URL、请求体、超时、降级）
 * 只在这里定义一份。</p>
 *
 * <p>降级策略：远程不可达、超时或返回空时一律返回空列表，不抛出异常，
 * 让对话在没有知识库上下文的情况下继续。</p>
 */
public class KnowledgeSearchClient {

	private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchClient.class);

	private final RestTemplate restTemplate;
	private final KnowledgeBaseProperties properties;

	public KnowledgeSearchClient(RestTemplate restTemplate, KnowledgeBaseProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	/**
	 * 静态工厂：按配置创建带超时的 {@link RestTemplate} 与客户端实例。
	 */
	public static KnowledgeSearchClient of(KnowledgeBaseProperties properties) {
		return new KnowledgeSearchClient(newRestTemplate(properties), properties);
	}

	/**
	 * 按配置创建带连接/读取超时的 {@link RestTemplate}。
	 */
	public static RestTemplate newRestTemplate(KnowledgeBaseProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(properties.getConnectTimeoutMillis());
		factory.setReadTimeout(properties.getReadTimeoutMillis());
		return new RestTemplate(factory);
	}

	/**
	 * 按 query 检索知识库；任何故障都降级为 {@link Collections#emptyList()}。
	 */
	public List<KnowledgeSearchResponse.Item> search(String query) {
		if (query == null || query.trim().isEmpty()) {
			return Collections.emptyList();
		}

		KnowledgeSearchRequest request =
				new KnowledgeSearchRequest(query, properties.getTopK(), properties.getFilters());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<KnowledgeSearchRequest> entity = new HttpEntity<>(request, headers);

		try {
			String url = properties.getBaseUrl() + properties.getSearchPath();
			log.debug("调用知识库接口: url={}, query={}, topK={}", url, query, properties.getTopK());

			ResponseEntity<KnowledgeSearchResponse> response = restTemplate.postForEntity(
					url, entity, KnowledgeSearchResponse.class);

			KnowledgeSearchResponse body = response.getBody();
			if (body == null || body.getItems() == null || body.getItems().isEmpty()) {
				log.debug("知识库未返回匹配条目: query={}", query);
				return Collections.emptyList();
			}
			return body.getItems();
		} catch (Exception e) {
			// 知识库故障不应打断对话：记录后按"无上下文"处理
			log.warn("知识库接口调用失败，本次按无知识库上下文继续: query={}, error={}", query, e.toString());
			return Collections.emptyList();
		}
	}

	/** 知识库检索请求体：{@code {"query":"...","topK":3}}。 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class KnowledgeSearchRequest {
		private final String query;
		private final int topK;
		private final Map<String, String> filters;

		public KnowledgeSearchRequest(String query, int topK) {
			this(query, topK, null);
		}

		public KnowledgeSearchRequest(String query, int topK, Map<String, String> filters) {
			this.query = query;
			this.topK = topK;
			this.filters = filters;
		}

		public String getQuery() {
			return query;
		}

		public int getTopK() {
			return topK;
		}

		public Map<String, String> getFilters() {
			return filters;
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
			private Map<String, Object> metadata;

			public String getTitle() {
				return title;
			}

			public Map<String, Object> getMetadata() {
				return metadata;
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
