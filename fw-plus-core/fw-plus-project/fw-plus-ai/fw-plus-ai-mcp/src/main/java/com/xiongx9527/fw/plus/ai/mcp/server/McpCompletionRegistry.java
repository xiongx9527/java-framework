package com.xiongx9527.fw.plus.ai.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionRef;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionResult;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 补全注册表
 *
 * <p>对齐 MCP 规范 completion/complete：
 * 每个 Provider 通过 {@link McpCompletionProvider#supportedRefs()} 声明可服务的引用集合，
 * 按引用类型 + 名称/URI 建立索引；调用时按请求 ref 派发到匹配的 Provider。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
class McpCompletionRegistry {
	private final Map<String, McpCompletionProvider> promptProviders = new ConcurrentHashMap<>();
	private final Map<String, McpCompletionProvider> resourceProviders = new ConcurrentHashMap<>();
	private final List<McpCompletionProvider> wildcardProviders =
			Collections.synchronizedList(new ArrayList<McpCompletionProvider>());

	void register(McpCompletionProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("Provider must not be null");
		}
		List<McpCompletionRef> refs = provider.supportedRefs();
		if (refs == null || refs.isEmpty()) {
			wildcardProviders.add(provider);
			return;
		}
		for (McpCompletionRef ref : refs) {
			if (ref == null) {
				continue;
			}
			String type = ref.type();
			if (McpCompletionRef.TYPE_PROMPT.equals(type)) {
				promptProviders.put(ref.name(), provider);
			} else if (McpCompletionRef.TYPE_RESOURCE.equals(type)) {
				resourceProviders.put(ref.uri(), provider);
			}
		}
	}

	McpCompletionProvider getProvider(McpCompletionRef ref) {
		if (ref == null) {
			return null;
		}
		String type = ref.type();
		MpCompletionMatch match = findMatch(type, ref);
		if (match != null) {
			return match.provider;
		}
		// fallback：匹配声明支持但未具名引用的 wildcard provider
		if (!wildcardProviders.isEmpty()) {
			return wildcardProviders.get(0);
		}
		return null;
	}

	private MpCompletionMatch findMatch(String type, McpCompletionRef ref) {
		if (McpCompletionRef.TYPE_PROMPT.equals(type)) {
			McpCompletionProvider provider = promptProviders.get(ref.name());
			if (provider != null) {
				return new MpCompletionMatch(provider);
			}
		} else if (McpCompletionRef.TYPE_RESOURCE.equals(type)) {
			McpCompletionProvider provider = resourceProviders.get(ref.uri());
			if (provider != null) {
				return new MpCompletionMatch(provider);
			}
		}
		return null;
	}

	List<McpCompletionProvider> listProviders() {
		List<McpCompletionProvider> all = new ArrayList<>();
		all.addAll(promptProviders.values());
		all.addAll(resourceProviders.values());
		all.addAll(wildcardProviders);
		return all;
	}

	boolean isEmpty() {
		return promptProviders.isEmpty() && resourceProviders.isEmpty() && wildcardProviders.isEmpty();
	}

	void clear() {
		promptProviders.clear();
		resourceProviders.clear();
		wildcardProviders.clear();
	}

	/**
	 * 执行补全
	 */
	McpCompletionResult complete(McpProviderContext context, McpCompletionRequest request) {
		McpCompletionRef ref = request.getRef();
		if (ref == null) {
			return McpCompletionResult.of(Collections.<String>emptyList());
		}
		McpCompletionProvider provider = getProvider(ref);
		if (provider == null) {
			return McpCompletionResult.of(Collections.<String>emptyList());
		}
		return provider.complete(context, request);
	}

	private static final class MpCompletionMatch {
		final McpCompletionProvider provider;

		MpCompletionMatch(McpCompletionProvider provider) {
			this.provider = provider;
		}
	}
}
