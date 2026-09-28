package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Magic-API → LangChain4j 本地工具提供者配置：{@code fw.plus.magicapi.langchain4j.tool.*}。
 *
 * <p>只控制本地 {@code ToolProvider} 的装配与工具范围；传输、MCP 等关注点不在此处。</p>
 */
@ConfigurationProperties(prefix = "fw.plus.magicapi.langchain4j.tool")
public class Langchain4jToolProperties {

	private boolean enabled = true;

	private Tools tools = new Tools();

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

	/**
	 * 两类资源的启用与过滤规则。
	 */
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

	/**
	 * 通用过滤范围：分组（{@link #includeGroups} 只保留、{@link #excludeGroups} 排除，Ant 匹配
	 * group path）与路径（{@link #includePaths} 只保留、{@link #excludePaths} 排除）。空列表表示不限。
	 *
	 * <p>{@link #inputSchemaAttribute} 声明入参 schema 元数据的属性名，默认 {@code mcpInputSchema}
	 * 与 MCP 插件共用：同一处元数据同时驱动 MCP 与 LangChain4j 两侧的工具参数。</p>
	 */
	public static class Scope {

		private boolean enabled = true;

		private List<String> includeGroups = new ArrayList<>();

		private List<String> excludeGroups = new ArrayList<>();

		private List<String> includePaths = new ArrayList<>();

		private List<String> excludePaths = new ArrayList<>();

		private String inputSchemaAttribute = "mcpInputSchema";

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

		public List<String> getExcludeGroups() {
			return excludeGroups;
		}

		public void setExcludeGroups(List<String> excludeGroups) {
			this.excludeGroups = excludeGroups;
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

		public String getInputSchemaAttribute() {
			return inputSchemaAttribute;
		}

		public void setInputSchemaAttribute(String inputSchemaAttribute) {
			this.inputSchemaAttribute = inputSchemaAttribute;
		}
	}

	/**
	 * api 专属过滤：HTTP 方法白名单与 opt-in 显式曝光。
	 *
	 * <p>{@link #optInOptionName} 默认与 MCP 插件共用 {@code mcpTool} 选项：同一处元数据同时
	 * 控制 MCP 与 LangChain4j 两侧的工具曝光。</p>
	 */
	public static class Api extends Scope {

		private List<String> methods = new ArrayList<>();

		private boolean requireOptIn = false;

		private String optInOptionName = "mcpTool";

		public Api() {
			setEnabled(true);
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
}
