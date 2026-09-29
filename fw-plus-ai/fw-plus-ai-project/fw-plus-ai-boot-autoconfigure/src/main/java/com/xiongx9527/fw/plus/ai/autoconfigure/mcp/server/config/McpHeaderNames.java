package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config;

import com.ajaxjs.mcp.server.ServerStreamableHttp;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * MCP header 名解析：四类协议 header（Accept、协议版本、会话 id、Origin）的名称可按配置改写，
 * 未配置时一律取内核默认名——即缺省行为与改造前逐字节一致。
 *
 * <p>网关统一改写 header 名时（如在代理层加前缀），用户只需在 {@code fw-plus.ai.mcp.headers}
 * 下声明自定义名称，入口即按自定义名读取请求、再以内核默认名透传给内核。</p>
 */
public final class McpHeaderNames {

	/** 内核默认会话 id 头名。 */
	public static final String DEFAULT_SESSION_ID = ServerStreamableHttp.SESSION_ID_HEADER;

	/** 内核默认协议版本头名。 */
	public static final String DEFAULT_PROTOCOL_VERSION = "MCP-Protocol-Version";

	/** 内核默认 Accept 头名。 */
	public static final String DEFAULT_ACCEPT = "Accept";

	/** 内核默认 Origin 头名。 */
	public static final String DEFAULT_ORIGIN = "Origin";

	private final McpProperties.Headers headers;

	public McpHeaderNames(McpProperties.Headers headers) {
		this.headers = headers == null ? new McpProperties.Headers() : headers;
	}

	/**
	 * 生效的会话 id 头名。
	 */
	public String sessionId() {
		return configuredOrDefault(headers.getSessionId(), DEFAULT_SESSION_ID);
	}

	/**
	 * 生效的协议版本头名。
	 */
	public String protocolVersion() {
		return configuredOrDefault(headers.getProtocolVersion(), DEFAULT_PROTOCOL_VERSION);
	}

	/**
	 * 生效的 Accept 头名。
	 */
	public String accept() {
		return configuredOrDefault(headers.getAccept(), DEFAULT_ACCEPT);
	}

	/**
	 * 生效的 Origin 头名。
	 */
	public String origin() {
		return configuredOrDefault(headers.getOrigin(), DEFAULT_ORIGIN);
	}

	/**
	 * 对入口收集到的 header 集合应用自定义名：配置名与默认名不同且请求确实带了自定义名时，
	 * 把该值同时以内核默认名写入，使内核与认证 SPI 无感知地按默认名读取。
	 *
	 * @param values 入口收集的 header（会被就地修改）
	 */
	public void applyCustomNames(Map<String, String> values) {
		if (values == null || values.isEmpty()) {
			return;
		}
		copyIfPresent(values, headers.getSessionId(), DEFAULT_SESSION_ID);
		copyIfPresent(values, headers.getProtocolVersion(), DEFAULT_PROTOCOL_VERSION);
		copyIfPresent(values, headers.getAccept(), DEFAULT_ACCEPT);
		copyIfPresent(values, headers.getOrigin(), DEFAULT_ORIGIN);
	}

	private void copyIfPresent(Map<String, String> values, String configuredName, String defaultName) {
		if (!StringUtils.hasText(configuredName) || configuredName.equals(defaultName)) {
			return;
		}
		String custom = values.get(configuredName);
		if (custom != null) {
			values.put(defaultName, custom);
		}
	}

	private String configuredOrDefault(String configured, String defaultName) {
		return StringUtils.hasText(configured) ? configured : defaultName;
	}
}
