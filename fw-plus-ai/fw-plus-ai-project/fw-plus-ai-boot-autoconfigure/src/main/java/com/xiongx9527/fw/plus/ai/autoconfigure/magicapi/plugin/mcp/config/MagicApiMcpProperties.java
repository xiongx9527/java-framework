package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Magic-API MCP 插件配置：{@code magic-api.plugin.mcp.*}。
 *
 * <p>只声明插件自身消费的键。传输与会话相关参数（endpoint/sessionTimeout/SSE/限流/信任源）
 * 由 mcp starter 的 {@code fw-plus.ai.mcp.*} 承担，此处不重复。</p>
 */
@ConfigurationProperties(prefix = "magic-api.plugin.mcp")
public class MagicApiMcpProperties {

	private boolean enabled = true;

	private Tools tools = new Tools();

	private List<Server> servers = new ArrayList<>();

	public static class Tools {

		private Scope functions = new Scope();

		private Api api = new Api();

		public Scope getFunctions() {
			return functions;
		}

		public void setFunctions(Scope functions) {
			this.functions = functions;
		}

		public Api getApi() {
			return api;
		}

		public void setApi(Api api) {
			this.api = api;
		}
	}

	public static class Scope {

		private boolean enabled = true;

		private List<String> includeGroups = new ArrayList<>();

		private List<String> includePaths = new ArrayList<>();

		private List<String> excludePaths = new ArrayList<>();

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public List<String> getIncludeGroups() {
			return includeGroups;
		}

		public void setIncludeGroups(List<String> includeGroups) {
			this.includeGroups = includeGroups;
		}

		public List<String> getIncludePaths() {
			return includePaths;
		}

		public void setIncludePaths(List<String> includePaths) {
			this.includePaths = includePaths;
		}

		public List<String> getExcludePaths() {
			return excludePaths;
		}

		public void setExcludePaths(List<String> excludePaths) {
			this.excludePaths = excludePaths;
		}
	}

	public static class Api extends Scope {

		private List<String> methods = new ArrayList<>();

		private boolean requireOptIn = false;

		private String optInOptionName = "mcpTool";

		public Api() {
			setEnabled(false);
		}

		public List<String> getMethods() {
			return methods;
		}

		public void setMethods(List<String> methods) {
			this.methods = methods;
		}

		public boolean isRequireOptIn() {
			return requireOptIn;
		}

		public void setRequireOptIn(boolean requireOptIn) {
			this.requireOptIn = requireOptIn;
		}

		public String getOptInOptionName() {
			return optInOptionName;
		}

		public void setOptInOptionName(String optInOptionName) {
			this.optInOptionName = optInOptionName;
		}
	}

	/**
	 * 插件侧 MCP server 实例配置：每段新增一个独立 server 实例（独立 endpoint 与工具过滤）。
	 */
	public static class Server {

		private String key;

		private String name;

		private String transport = "http-sse";

		private String endpoint;

		private Tools tools;

		private Auth auth;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTransport() {
			return transport;
		}

		public void setTransport(String transport) {
			this.transport = transport;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public Tools getTools() {
			return tools;
		}

		public void setTools(Tools tools) {
			this.tools = tools;
		}

		public Auth getAuth() {
			return auth;
		}

		public void setAuth(Auth auth) {
			this.auth = auth;
		}
	}

	public static class Auth {

		private String provider;

		public String getProvider() {
			return provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}


	public Tools getTools() {
		return tools;
	}

	public void setTools(Tools tools) {
		this.tools = tools;
	}

	public List<Server> getServers() {
		return servers;
	}

	public void setServers(List<Server> servers) {
		this.servers = servers;
	}
}
