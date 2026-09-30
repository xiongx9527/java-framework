package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model.Langchain4jMcpClientInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code langchain4j-mcp-client} 数据源的实例持有者：按 {@code key} 持有多个 MCP 客户端，
 * 每个客户端包装成 {@link McpToolProvider} 对外提供工具。
 *
 * <p>实例销毁（下线/更新）时同步关闭底层 {@link McpClient}，释放 SSE/子进程/容器资源。</p>
 */
public class MagicDynamicLangchain4jMcpClientDataSource implements DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(MagicDynamicLangchain4jMcpClientDataSource.class);

	private static final String DEFAULT_CLIENT_NAME = "magic-langchain4j";

	private final Map<String, McpClient> clients = new ConcurrentHashMap<>();

	private final Map<String, ToolProvider> providers = new ConcurrentHashMap<>();

	/** 各实例最近一次的资源定义，手动 refresh(key) 重建时使用 */
	private final Map<String, Langchain4jMcpClientInfo> infos = new ConcurrentHashMap<>();

	/** 动态头函数结果缓存：key → 最近一次解析结果（headers-cache-seconds 生效时使用） */
	private final Map<String, CachedHeaders> headerCaches = new ConcurrentHashMap<>();

	/** magic-api function 执行器：动态请求头函数走它调用 */
	private final MagicToolInvoker invoker;

	public MagicDynamicLangchain4jMcpClientDataSource(MagicToolInvoker invoker) {
		this.invoker = invoker == null ? new com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.ScriptManagerMagicToolInvoker() : invoker;
	}

	/**
	 * 按资源定义创建（或重建）一个 MCP 客户端与其 ToolProvider。
	 *
	 * @param info 资源定义
	 * @return 创建成功返回 {@code true}
	 */
	public boolean create(Langchain4jMcpClientInfo info) {
		String key = info.getKey();
		if (key == null || key.trim().isEmpty()) {
			return false;
		}
		try {
			destroy(key);
			McpTransport transport = buildTransport(info);
			DefaultMcpClient.Builder clientBuilder = DefaultMcpClient.builder()
					.clientName(info.getClientName() == null || info.getClientName().trim().isEmpty()
							? DEFAULT_CLIENT_NAME : info.getClientName())
					.transport(transport);
			if (info.getListenerFunction() != null && !info.getListenerFunction().trim().isEmpty()) {
				clientBuilder.addListener(buildListener(key, info.getListenerFunction().trim()));
			}
			DefaultMcpClient client = clientBuilder.build();
			McpToolProvider.Builder builder = McpToolProvider.builder()
					.mcpClients(Collections.singletonList(client))
					.failIfOneServerFails(info.getFailIfOneServerFails() != null
							&& info.getFailIfOneServerFails());
			if (info.getFilterToolNames() != null && !info.getFilterToolNames().isEmpty()) {
				builder.filterToolNames(info.getFilterToolNames());
			}
			clients.put(key, client);
			infos.put(key, info);
			providers.put(key, wrapWithAuthRetry(builder.build(), key, info));
			logger.info("注册 LangChain4j MCP 客户端: {} -> {}", key, info.getTransport());
			return true;
		} catch (Throwable e) {
			logger.warn("构建 LangChain4j MCP 客户端失败: key={}", key, e);
			return false;
		}
	}

	/** 按传输类型构建底层传输。 */
	private McpTransport buildTransport(Langchain4jMcpClientInfo info) {
		String type = info.getTransport() == null || info.getTransport().trim().isEmpty()
				? "http" : info.getTransport().trim();
		Map<String, String> environment = info.getEnvironment();
		switch (type) {
			case "http":
				if (info.getUrl() == null || info.getUrl().trim().isEmpty()) {
					throw new IllegalArgumentException("http 传输必须配置 url");
				}
				StreamableHttpMcpTransport.Builder httpBuilder = new StreamableHttpMcpTransport.Builder()
						.url(info.getUrl());
				boolean hasStaticHeaders = info.getCustomHeaders() != null && !info.getCustomHeaders().isEmpty();
				boolean hasDynamicHeaders = info.getCustomHeadersFunction() != null
						&& !info.getCustomHeadersFunction().trim().isEmpty();
				if (hasDynamicHeaders) {
					// 每次请求动态取头（如调 token 接口换 api-key）；函数失败回落静态头。
					// 函数返回 Map 的特殊键 _url 表示最新请求 URL（query 放 api-key 的服务），
					// 值变化即异步按新 URL 重建客户端
					String functionName = info.getCustomHeadersFunction().trim();
					Map<String, String> staticHeaders = hasStaticHeaders ? info.getCustomHeaders() : null;
					httpBuilder.customHeaders((java.util.function.Supplier<Map<String, String>>) () ->
							resolveHeaders(info.getKey(), info, functionName, staticHeaders));
				} else if (hasStaticHeaders) {
					httpBuilder.customHeaders(info.getCustomHeaders());
				}
				return httpBuilder.build();
			case "stdio":
				if (info.getCommand() == null || info.getCommand().isEmpty()) {
					throw new IllegalArgumentException("stdio 传输必须配置 command");
				}
				return new StdioMcpTransport.Builder()
						.command(info.getCommand())
						.environment(environment)
						.logEvents(false)
						.build();
			case "docker":
				if (info.getImage() == null || info.getImage().trim().isEmpty()) {
					throw new IllegalArgumentException("docker 传输必须配置 image");
				}
				dev.langchain4j.mcp.client.transport.docker.DockerMcpTransport.Builder builder =
						new dev.langchain4j.mcp.client.transport.docker.DockerMcpTransport.Builder()
								.image(info.getImage());
				if (info.getCommand() != null && !info.getCommand().isEmpty()) {
					builder.command(info.getCommand());
				}
				if (environment != null && !environment.isEmpty()) {
					builder.environment(environment);
				}
				return builder.build();
			default:
				throw new IllegalArgumentException(
						String.format("不支持的传输类型 '%s'（可选 http / stdio / docker）", type));
		}
	}

	/** 动态头函数返回 Map 中的特殊键：最新请求 URL（query 放 api-key 的服务），值变化即重建客户端 */
	private static final String DYNAMIC_URL_KEY = "_url";

	/**
	 * 构建消息监听器：把 MCP 客户端的关键生命周期事件与服务端通知转发给
	 * magic-api function（参数 {@code {key, event, detail}}），函数异常只记日志不影响链路。
	 */
	private McpClientListener buildListener(String key, String function) {
		return new McpClientListener() {
			@Override
			public void afterInitialize(dev.langchain4j.mcp.client.McpCallContext context) {
				fire("initialized", null);
			}

			@Override
			public void onInitializeError(dev.langchain4j.mcp.client.McpCallContext context, Throwable error) {
				fire("initialize.error", String.valueOf(error));
				invalidateCacheIfAuthError(key, error);
			}

			@Override
			public void afterExecuteTool(dev.langchain4j.mcp.client.McpCallContext context,
					dev.langchain4j.service.tool.ToolExecutionResult result,
					Map<String, Object> toolArgs) {
				fire("tool.executed", toolArgs);
			}

			@Override
			public void onExecuteToolError(dev.langchain4j.mcp.client.McpCallContext context, Throwable error) {
				fire("tool.error", String.valueOf(error));
				invalidateCacheIfAuthError(key, error);
			}

			@Override
			public void onNotificationToolsListChanged() {
				fire("tools.list.changed", null);
			}

			@Override
			public void onNotificationResourcesListChanged() {
				fire("resources.list.changed", null);
			}

			@Override
			public void onNotificationPromptsListChanged() {
				fire("prompts.list.changed", null);
			}

			@Override
			public void onNotificationResourceUpdated(String resource) {
				fire("resource.updated", resource);
			}

			@Override
			public void onNotificationMessage(dev.langchain4j.mcp.client.logging.McpLogMessage message) {
				fire("log", String.valueOf(message));
			}

			@Override
			public void onNotificationProgress(dev.langchain4j.mcp.client.progress.McpProgressNotification progress) {
				fire("progress", String.valueOf(progress));
			}

			@Override
			public void onNotificationCancelled(long requestId, String reason) {
				fire("cancelled", reason);
			}

			private void fire(String event, Object detail) {
				try {
					Map<String, Object> args = new java.util.HashMap<>();
					args.put("key", key);
					args.put("event", event);
					if (detail != null) {
						args.put("detail", detail);
					}
					invoker.invokeFunction(function, args);
				} catch (Exception e) {
					logger.warn("MCP 监听函数 {} 调用失败: key={}, event={}", function, key, event, e);
				}
			}
		};
	}

	/** api-key 过期的错误特征：命中即作废动态头缓存，下次请求重新调函数取新 key */
	private static boolean isAuthError(Throwable error) {
		if (error == null) {
			return false;
		}
		String message = String.valueOf(error.getMessage()).toLowerCase();
		return message.contains("401") || message.contains("403") || message.contains("unauthorized")
				|| message.contains("forbidden") || message.contains("invalid api key")
				|| message.contains("api key") || message.contains("apikey")
				|| message.contains("token expired") || message.contains("token 已过期");
	}

	/** 认证类失败时作废动态头缓存：TTL 内的过期 key 不再拖到过期时间 */
	private void invalidateCacheIfAuthError(String key, Throwable error) {
		if (isAuthError(error) && headerCaches.remove(key) != null) {
			logger.info("MCP 客户端 {} 认证失败，已作废动态头缓存，下次请求重新获取 api-key", key);
		}
	}

	/**
	 * 解析动态请求头：调 magic-api function 取最新头（如换新 api-key），
	 * 与静态头合并（动态覆盖同名 key）；函数失败时回落静态头并告警。
	 *
	 * <p>返回 Map 若带 {@link #DYNAMIC_URL_KEY}，取其值与当前 url 比较，
	 * 变化则异步重建该实例（transport 的 url 不可变，query 轮换 key 只能重建）。</p>
	 */
	private Map<String, String> resolveHeaders(String key, Langchain4jMcpClientInfo info,
			String functionName, Map<String, String> staticHeaders) {
		long ttlSeconds = info.getHeadersCacheSeconds() == null ? 0 : info.getHeadersCacheSeconds();
		CachedHeaders cached = ttlSeconds > 0 ? headerCaches.get(key) : null;
		long now = System.currentTimeMillis();
		if (cached != null && now - cached.fetchedAt < ttlSeconds * 1000L) {
			return cached.headers;
		}
		Map<String, String> merged = new java.util.HashMap<>();
		if (staticHeaders != null) {
			merged.putAll(staticHeaders);
		}
		String newUrl = null;
		try {
			Object result = invoker.invokeFunction(functionName, Collections.<String, Object>emptyMap());
			if (result instanceof Map) {
				for (Object entry : ((Map<?, ?>) result).entrySet()) {
					Object k = ((Map.Entry<?, ?>) entry).getKey();
					Object v = ((Map.Entry<?, ?>) entry).getValue();
					if (k == null || v == null) {
						continue;
					}
					if (DYNAMIC_URL_KEY.equals(String.valueOf(k))) {
						newUrl = String.valueOf(v);
					} else {
						merged.put(String.valueOf(k), String.valueOf(v));
					}
				}
			} else {
				logger.warn("动态请求头函数 {} 返回非 Map（{}），忽略", functionName,
						result == null ? "null" : result.getClass().getName());
			}
		} catch (Exception e) {
			logger.warn("动态请求头函数 {} 调用失败，使用静态头", functionName, e);
		}
		Map<String, String> resolved = Collections.unmodifiableMap(merged);
		if (ttlSeconds > 0) {
			headerCaches.put(key, new CachedHeaders(now, resolved));
		}
		if (newUrl != null && !newUrl.equals(info.getUrl())) {
			rebuildWithUrl(key, info, newUrl);
		}
		return resolved;
	}

	/** 动态头函数解析结果的缓存条目。 */
	private static final class CachedHeaders {

		private final long fetchedAt;

		private final Map<String, String> headers;

		private CachedHeaders(long fetchedAt, Map<String, String> headers) {
			this.fetchedAt = fetchedAt;
			this.headers = headers;
		}
	}

	/**
	 * 认证失败自动重试包装：工具执行遇到认证类异常（api-key 过期等）时，作废动态头缓存
	 * （下次 Supplier 取新 key）并按 {@code auth-retry-count} 重试；非认证异常直接抛出。
	 */
	private ToolProvider wrapWithAuthRetry(ToolProvider inner, String key, Langchain4jMcpClientInfo info) {
		int retries = info.getAuthRetryCount() == null ? 0 : Math.max(0, info.getAuthRetryCount());
		if (retries <= 0) {
			return inner;
		}
		return request -> {
			ToolProviderResult result = inner.provideTools(request);
			if (result == null || result.tools() == null || result.tools().isEmpty()) {
				return result;
			}
			ToolProviderResult.Builder wrapped = ToolProviderResult.builder();
			result.tools().forEach((spec, executor) -> wrapped.add(spec, (req, memoryId) -> {
				RuntimeException last = null;
				for (int attempt = 0; attempt <= retries; attempt++) {
					try {
						return executor.execute(req, memoryId);
					} catch (RuntimeException e) {
						if (!isAuthError(e)) {
							throw e;
						}
						last = e;
						headerCaches.remove(key);
						logger.warn("MCP 工具 {} 认证失败（第 {}/{} 次尝试），已作废 api-key 缓存",
								spec.name(), attempt + 1, retries + 1);
						if (attempt < retries) {
							try {
								Thread.sleep(200L * (attempt + 1));
							} catch (InterruptedException ie) {
								Thread.currentThread().interrupt();
								throw e;
							}
						}
					}
				}
				throw last;
			}));
			return wrapped.build();
		};
	}

	/** 检测到动态 url 变化后，异步销毁旧客户端并按新 URL 重建（避免在请求回调线程中重入）。 */
	private void rebuildWithUrl(String key, Langchain4jMcpClientInfo info, String newUrl) {
		logger.info("MCP 客户端 {} 的动态 url 变化，异步重建: {} -> {}", key, info.getUrl(), newUrl);
		Thread thread = new Thread(() -> {
			try {
				Langchain4jMcpClientInfo copy = new Langchain4jMcpClientInfo();
				copy.setKey(info.getKey());
				copy.setName(info.getName());
				copy.setEnabled(true);
				copy.setTransport(info.getTransport());
				copy.setUrl(newUrl);
				copy.setCommand(info.getCommand());
				copy.setEnvironment(info.getEnvironment());
				copy.setImage(info.getImage());
				copy.setClientName(info.getClientName());
				copy.setCustomHeaders(info.getCustomHeaders());
				copy.setCustomHeadersFunction(info.getCustomHeadersFunction());
				copy.setFilterToolNames(info.getFilterToolNames());
				copy.setFailIfOneServerFails(info.getFailIfOneServerFails());
				copy.setHeadersCacheSeconds(info.getHeadersCacheSeconds());
				copy.setAuthRetryCount(info.getAuthRetryCount());
				create(copy);
			} catch (Exception e) {
				logger.warn("MCP 客户端 {} 按新 url 重建失败", key, e);
			}
		}, "magic-mcp-rebuild-" + key);
		thread.setDaemon(true);
		thread.start();
	}

	/** 按 key 销毁实例（关闭底层客户端，释放连接/子进程/容器）。 */
	public void destroy(String key) {
		if (key == null) {
			return;
		}
		providers.remove(key);
		headerCaches.remove(key);
		McpClient client = clients.remove(key);
		if (client != null) {
			try {
				client.close();
			} catch (Exception e) {
				logger.warn("关闭 LangChain4j MCP 客户端失败: key={}", key, e);
			}
		}
	}

	/** 按 key 取 ToolProvider，不存在时返回 {@code null}。 */
	public ToolProvider get(String key) {
		return key == null ? null : providers.get(key);
	}

	/**
	 * 手动刷新：销毁并按最近一次资源定义重建指定实例——重新连接 MCP Server、
	 * 重新调 headers-function 取 api-key、重新拉取工具列表。刷新失败时抛出异常。
	 *
	 * @return 刷新后的 ToolProvider
	 */
	public ToolProvider refresh(String key) {
		Langchain4jMcpClientInfo info = key == null ? null : infos.get(key);
		if (info == null) {
			throw new IllegalArgumentException(String.format(
					"未配置 LangChain4j MCP 客户端: %s，无法刷新", key));
		}
		destroy(key);
		if (!create(info)) {
			throw new IllegalStateException(String.format("刷新 LangChain4j MCP 客户端失败: %s", key));
		}
		return providers.get(key);
	}

	/** 仅使指定实例的工具列表缓存失效：下次提供工具时重新 listTools，不重连。 */
	public void evictToolListCache(String key) {
		McpClient client = key == null ? null : clients.get(key);
		if (client instanceof DefaultMcpClient) {
			((DefaultMcpClient) client).evictToolListCache();
		}
	}

	/** 当前已配置的 key 集合（不可修改视图）。 */
	public Set<String> keys() {
		return Collections.unmodifiableSet(providers.keySet());
	}

	@Override
	public void destroy() {
		for (String key : clients.keySet().toArray(new String[0])) {
			destroy(key);
		}
	}
}
