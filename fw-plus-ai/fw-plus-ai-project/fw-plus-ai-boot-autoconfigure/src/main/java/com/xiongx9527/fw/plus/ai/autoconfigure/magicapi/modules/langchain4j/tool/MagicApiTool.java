package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Magic-API 资源派生出的一个 LLM 工具快照项。
 *
 * <p>覆盖两类资源：函数（{@link #TYPE_FUNCTION}）与 HTTP API（{@link #TYPE_API}）。
 * API 额外携带 {@link #getMethod()}（GET/POST/...）与 {@link #getPath()}，函数无 method 语义。
 * 工具名采用 MCP 风格原样名：api 为 {@code "POST /path"}，function 为函数名。</p>
 */
public class MagicApiTool {

	public static final String TYPE_FUNCTION = "function";

	public static final String TYPE_API = "api";

	private final String id;

	private final String name;

	private final String description;

	private final String type;

	private final String method;

	private final String path;

	private final String groupId;

	private final String groupPath;

	private final Map<String, Object> inputSchema;

	public MagicApiTool(String id, String name, String description, String type, String method, String path,
					   String groupId, String groupPath, Map<String, Object> inputSchema) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.type = type;
		this.method = method;
		this.path = path;
		this.groupId = groupId;
		this.groupPath = groupPath;
		this.inputSchema = inputSchema == null ? new LinkedHashMap<>() : inputSchema;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getType() {
		return type;
	}

	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getGroupPath() {
		return groupPath;
	}

	public Map<String, Object> getInputSchema() {
		return inputSchema;
	}
}
