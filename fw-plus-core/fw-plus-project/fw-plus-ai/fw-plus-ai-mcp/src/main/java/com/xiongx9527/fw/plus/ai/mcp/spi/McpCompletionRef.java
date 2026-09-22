package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 补全引用
 *
 * <p>对齐 MCP 规范 completion/complete 的 ref 参数：
 * <ul>
 *   <li>{@code ref/prompt}: {@code {type:"ref/prompt", name:"..."}}</li>
 *   <li>{@code ref/resource}: {@code {type:"ref/resource", uri:"..."}}</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpCompletionRef {
	public static final String TYPE_PROMPT = "ref/prompt";
	public static final String TYPE_RESOURCE = "ref/resource";

	private String type;
	private String name; // for ref/prompt
	private String uri; // for ref/resource

	public McpCompletionRef() {
	}

	public McpCompletionRef(String type, String name) {
		this.type = type;
		if (TYPE_RESOURCE.equals(type)) {
			this.uri = name;
		} else {
			this.name = name;
		}
	}

	public static McpCompletionRef prompt(String name) {
		McpCompletionRef ref = new McpCompletionRef();
		ref.type = TYPE_PROMPT;
		ref.name = name;
		return ref;
	}

	public static McpCompletionRef resource(String uri) {
		McpCompletionRef ref = new McpCompletionRef();
		ref.type = TYPE_RESOURCE;
		ref.uri = uri;
		return ref;
	}

	public String type() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String name() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String uri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("type", type);
		if (TYPE_PROMPT.equals(type) && name != null) {
			map.put("name", name);
		}
		if (TYPE_RESOURCE.equals(type) && uri != null) {
			map.put("uri", uri);
		}
		return map;
	}
}
