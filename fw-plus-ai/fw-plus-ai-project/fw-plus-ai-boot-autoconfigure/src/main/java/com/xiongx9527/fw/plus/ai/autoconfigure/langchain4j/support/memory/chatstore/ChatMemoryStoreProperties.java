package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * ChatMemoryStore 存储配置项。
 *
 * <p>{@code type} 决定注入哪种存储后端：{@code in-memory}（默认，进程内兜底）/
 * {@code mem0}（mem0 兼容 REST 服务，本地部署或云端均可，连接参数见 {@link Mem0}）。</p>
 *
 * <p>memId 契约为 {@code {tenantId}:{appId}:{agentId}:{userId}} 四段拼接
 * （见 {@link ChatMemoryIds}），段数不足时各维度回退以下默认值。</p>
 */
@ConfigurationProperties(ChatMemoryStoreProperties.PREFIX)
public class ChatMemoryStoreProperties {

	public static final String PREFIX = "fw.plus.ai.langchain4j.chat-memory";

	public static final String TYPE_IN_MEMORY = "in-memory";

	public static final String TYPE_MEM0 = "mem0";

	/** 存储后端类型，默认进程内存储 */
	private String type = TYPE_IN_MEMORY;

	/** memId 段[1]（app_id）缺失时的默认值 */
	private String defaultAppId = ChatMemoryIds.DEFAULT_APP_ID;

	/** memId 段[0]（tenant_id）缺失且租户解析器未提供时的默认值 */
	private String defaultTenantId = ChatMemoryStoreDefaults.DEFAULT_TENANT_ID;

	private final Mem0 mem0 = new Mem0();

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDefaultAppId() {
		return defaultAppId;
	}

	public void setDefaultAppId(String defaultAppId) {
		this.defaultAppId = defaultAppId;
	}

	public String getDefaultTenantId() {
		return defaultTenantId;
	}

	public void setDefaultTenantId(String defaultTenantId) {
		this.defaultTenantId = defaultTenantId;
	}

	public Mem0 getMem0() {
		return mem0;
	}

	/** mem0 REST 存储配置 */
	public static class Mem0 {

		/** 默认服务根地址（本地部署） */
		public static final String DEFAULT_BASE_URL = "http://localhost:8000";

		/**
		 * mem0 服务根地址（可含版本前缀）：部署在根路径写 {@code http://host:8000}，
		 * 网关要求 {@code /v1} 前缀时写 {@code http://host:8000/v1}
		 */
		private String baseUrl = DEFAULT_BASE_URL;

		/** API Key；本地部署未开启鉴权时留空（不发送 Authorization 头） */
		private String apiKey = "";

		/** 连接超时，默认 5s */
		private Duration connectTimeout = Duration.ofSeconds(5);

		/** 读取超时，默认 10s */
		private Duration readTimeout = Duration.ofSeconds(10);

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public Duration getConnectTimeout() {
			return connectTimeout;
		}

		public void setConnectTimeout(Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		public Duration getReadTimeout() {
			return readTimeout;
		}

		public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}
	}
}
