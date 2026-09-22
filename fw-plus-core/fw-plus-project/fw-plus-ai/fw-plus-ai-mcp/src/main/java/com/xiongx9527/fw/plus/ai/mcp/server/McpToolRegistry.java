package com.xiongx9527.fw.plus.ai.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.spi.McpTool;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
class McpToolRegistry {
	private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
	private final Map<String, McpToolProvider> providers = new ConcurrentHashMap<>();

	void register(McpTool tool, McpToolProvider provider) {
		if (tool == null || provider == null) {
			throw new IllegalArgumentException("Tool and provider must not be null");
		}
		tools.put(tool.name(), tool);
		providers.put(tool.name(), provider);
	}

	McpTool getTool(String name) {
		return tools.get(name);
	}

	McpToolProvider getProvider(String name) {
		return providers.get(name);
	}

	Collection<McpTool> listTools() {
		return Collections.unmodifiableCollection(tools.values());
	}

	boolean isEmpty() {
		return tools.isEmpty();
	}

	void clear() {
		tools.clear();
		providers.clear();
	}
}
