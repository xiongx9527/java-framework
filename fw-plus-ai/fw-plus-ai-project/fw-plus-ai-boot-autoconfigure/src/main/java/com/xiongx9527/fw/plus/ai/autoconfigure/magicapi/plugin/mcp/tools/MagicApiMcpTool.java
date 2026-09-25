package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 已过滤工具快照中的一个工具。「快照」即 {@link MagicApiMcpToolRegistry#snapshot()}
 * 在某一时刻产出的不可变值对象，后续由 MCP 工具曝光（tools/list、tools/call）消费。
 *
 * <p>同时覆盖 Magic-API 两类资源：函数（{@link #TYPE_FUNCTION}）与 HTTP API
 * （{@link #TYPE_API}）。API 额外携带 {@link #getMethod()}（GET/POST/...）与
 * {@link #getPath()}，函数无 method 语义。</p>
 *
 * <p>实现 {@code equals}/{@code hashCode} 以便 3.4 增量 diff 判断快照是否变化。</p>
 */
public class MagicApiMcpTool {

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

	private final boolean optIn;

	private final Map<String, Object> inputSchema;

	public MagicApiMcpTool(String id, String name, String description, String type, String method, String path,
						  String groupId, String groupPath, boolean optIn, Map<String, Object> inputSchema) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.type = type;
		this.method = method;
		this.path = path;
		this.groupId = groupId;
		this.groupPath = groupPath;
		this.optIn = optIn;
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

	public boolean isOptIn() {
		return optIn;
	}

	public Map<String, Object> getInputSchema() {
		return inputSchema;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MagicApiMcpTool that = (MagicApiMcpTool) o;
		return optIn == that.optIn && Objects.equals(id, that.id) && Objects.equals(name, that.name)
				&& Objects.equals(description, that.description) && Objects.equals(type, that.type)
				&& Objects.equals(method, that.method) && Objects.equals(path, that.path)
				&& Objects.equals(groupId, that.groupId) && Objects.equals(groupPath, that.groupPath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, type, method, path, groupId, groupPath, optIn);
	}
}
