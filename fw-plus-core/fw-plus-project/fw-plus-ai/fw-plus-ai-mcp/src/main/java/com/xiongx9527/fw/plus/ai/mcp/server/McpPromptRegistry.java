package com.xiongx9527.fw.plus.ai.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提示词注册表
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
class McpPromptRegistry {
	private final Map<String, McpPromptInfo> prompts = new ConcurrentHashMap<>();
	private final Map<String, McpPromptProvider> providers = new ConcurrentHashMap<>();

	void register(McpPromptInfo prompt, McpPromptProvider provider) {
		if (prompt == null || provider == null) {
			throw new IllegalArgumentException("Prompt and provider must not be null");
		}
		prompts.put(prompt.name(), prompt);
		providers.put(prompt.name(), provider);
	}

	McpPromptInfo getPrompt(String name) {
		return prompts.get(name);
	}

	McpPromptProvider getProvider(String name) {
		return providers.get(name);
	}

	Collection<McpPromptInfo> listPrompts() {
		return Collections.unmodifiableCollection(prompts.values());
	}

	boolean isEmpty() {
		return prompts.isEmpty();
	}

	void clear() {
		prompts.clear();
		providers.clear();
	}
}
