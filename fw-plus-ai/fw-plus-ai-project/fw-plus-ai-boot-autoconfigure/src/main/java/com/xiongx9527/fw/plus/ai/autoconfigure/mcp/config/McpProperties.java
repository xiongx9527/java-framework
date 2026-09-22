package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP 配置属性
 *
 * <p>对齐 spec 8.1：聚合各实例配置 {@link McpInstanceProperties}，
 * 通过 {@code fw-plus.mcp.*} 前缀绑定。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
@ConfigurationProperties(prefix = "fw-plus.ai.mcp")
public class McpProperties {

	private boolean enabled = true;
	private boolean sseEnabled = true;
	private boolean websocketEnabled = false;
	private String defaultInstance = "default";
	private List<String> allowedOrigins = new ArrayList<>();
	private List<McpInstanceProperties> instances = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getDefaultInstance() {
		return defaultInstance;
	}

	public void setDefaultInstance(String defaultInstance) {
		this.defaultInstance = defaultInstance;
	}

	/**
	 * SSE 端点允许的 {@code Origin} 白名单
	 *
	 * <p>为空时按"同源 + 环回地址"放行；配置 {@code *} 表示不限制来源；
	 * 未命中白名单的跨域请求按 spec 5.1.6 返回 HTTP 403。
	 */
	public List<String> getAllowedOrigins() {
		return allowedOrigins;
	}

	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	public List<McpInstanceProperties> getInstances() {
		return instances;
	}

	public void setInstances(List<McpInstanceProperties> instances) {
		this.instances = instances;
	}

	public McpInstanceProperties getInstance(String name) {
		if (instances != null) {
			for (McpInstanceProperties instance : instances) {
				if (name.equals(instance.getName())) {
					return instance;
				}
			}
		}
		return null;
	}
}
