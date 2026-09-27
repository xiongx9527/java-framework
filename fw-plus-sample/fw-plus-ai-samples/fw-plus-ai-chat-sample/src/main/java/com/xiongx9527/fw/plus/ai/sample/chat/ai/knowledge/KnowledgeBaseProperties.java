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
}
