package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.spi;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpTool;

import java.util.Collections;
import java.util.List;

/**
 * MCP 能力自定义提供者 SPI：与 {@code mcp-resource}/{@code mcp-prompt} 等动态资源类型的能力并存聚合。
 *
 * <p>按需实现需要的方法，未实现的方法由默认返回空集合。同一标识（工具名、资源 uri、提示词名）
 * 同时来自资源定义与 provider 时，provider 优先并在日志中标注覆盖。</p>
 */
public interface MagicApiMcpCapabilityProvider {

	/**
	 * 自定义工具。
	 *
	 * @return 工具集合，不会为 {@code null}
	 */
	default List<MagicApiMcpTool> mcpTools() {
		return Collections.emptyList();
	}

	/**
	 * 自定义资源。
	 *
	 * @return 资源定义集合，不会为 {@code null}
	 */
	default List<MagicMcpResourceInfo> mcpResources() {
		return Collections.emptyList();
	}

	/**
	 * 自定义提示词。
	 *
	 * @return 提示词定义集合，不会为 {@code null}
	 */
	default List<MagicMcpPromptInfo> mcpPrompts() {
		return Collections.emptyList();
	}

	/**
	 * 自定义补全候选。
	 *
	 * @return 补全条目集合，不会为 {@code null}
	 */
	default List<CompletionEntry> mcpCompletions() {
		return Collections.emptyList();
	}

	/**
	 * 一条补全候选：{@code (referenceType, referenceName, argumentName) → values}。
	 */
	class CompletionEntry {

		private final String referenceType;

		private final String referenceName;

		private final String argumentName;

		private final List<String> values;

		public CompletionEntry(String referenceType, String referenceName, String argumentName,
				List<String> values) {
			this.referenceType = referenceType;
			this.referenceName = referenceName;
			this.argumentName = argumentName;
			this.values = values == null ? Collections.<String>emptyList() : values;
		}

		/**
		 * 内核 completion store 的 key：{@code type:name:argument}。
		 *
		 * @return store key
		 */
		public String storeKey() {
			return referenceType + ":" + referenceName + ":" + argumentName;
		}

		public String getReferenceType() {
			return referenceType;
		}

		public String getReferenceName() {
			return referenceName;
		}

		public String getArgumentName() {
			return argumentName;
		}

		public List<String> getValues() {
			return values;
		}
	}
}
