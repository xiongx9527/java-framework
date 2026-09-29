package com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 远程知识库接口配置。
 *
 * <p>前缀 {@value #PREFIX}；未配置 {@code base-url} 时知识库相关 Bean 不装配，
 * 样例退化为纯对话，保证开箱可跑。</p>
 */
@ConfigurationProperties(prefix = KnowledgeBaseProperties.PREFIX)
public class KnowledgeBaseProperties {

	public static final String PREFIX = "fw.plus.sample.chat.knowledge-base";

	/** 知识库服务地址，例如 {@code http://localhost:8081}。 */
	private String baseUrl;

	/** 检索路径，默认 {@code /api/search}。 */
	private String searchPath = "/api/search";

	/** 每次检索返回的最大条目数。 */
	private int topK = 3;

	/** 相关度下限：低于此分数的条目不进提示词。知识库兜底返回的无关文档靠它挡掉。 */
	private double minScore = 0.5;

	/** 元数据过滤条件，如 {@code category=travel}、{@code tag=天气}；空表示不过滤。 */
	private java.util.Map<String, String> filters = new java.util.LinkedHashMap<>();

	/** 连接超时（毫秒）。 */
	private int connectTimeoutMillis = 2000;

	/** 读取超时（毫秒）。 */
	private int readTimeoutMillis = 5000;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getSearchPath() {
		return searchPath;
	}

	public void setSearchPath(String searchPath) {
		this.searchPath = searchPath;
	}

	public int getTopK() {
		return topK;
	}

	public void setTopK(int topK) {
		this.topK = topK;
	}

	public int getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public void setConnectTimeoutMillis(int connectTimeoutMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
	}

	public int getReadTimeoutMillis() {
		return readTimeoutMillis;
	}

	public void setReadTimeoutMillis(int readTimeoutMillis) {
		this.readTimeoutMillis = readTimeoutMillis;
	}

	public double getMinScore() {
		return minScore;
	}

	public void setMinScore(double minScore) {
		this.minScore = minScore;
	}

	public java.util.Map<String, String> getFilters() {
		return filters;
	}

	public void setFilters(java.util.Map<String, String> filters) {
		this.filters = filters;
	}
}
