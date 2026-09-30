package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.model;

import com.ajaxjs.mcp.client.McpClient;
import com.ajaxjs.mcp.client.transport.HttpMcpTransport;
import com.ajaxjs.mcp.client.transport.StreamableHttpTransport;
import com.ajaxjs.mcp.client.transport.StdioTransport;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code langchain4j-aj-mcp-client} 数据源的实例持有者：基于 aj-mcp（Java 8 兼容 SDK）按
 * {@code key} 持有多个 MCP 客户端，把外部 MCP Server 的工具适配成 LangChain4j 的
 * {@link ToolProvider}（0.35 API）。
 *
 * <p>实例销毁（下线/更新）时同步 {@code close()} 底层 aj-mcp 客户端，释放 SSE/子进程资源。</p>
 */
public class MagicDynamicLangchain4jAjMcpClientDataSource implements DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(MagicDynamicLangchain4jAjMcpClientDataSource.class);

	private final Map<String, McpClient> clients = new ConcurrentHashMap<String, McpClient>();

	private final Map<String, ToolProvider> providers = new ConcurrentHashMap<String, ToolProvider>();

	/** 各实例最近一次的资源定义，手动 refresh(key) 重建时使用 */
	private final Map<String, Langchain4jAjMcpClientInfo> infos = new ConcurrentHashMap<String, Langchain4jAjMcpClientInfo>();

	/**
	 * 按资源定义创建（或重建）一个 aj-mcp 客户端与其 ToolProvider。
	 *
	 * @param info 资源定义
	 * @return 创建成功返回 {@code true}
	 */
	public boolean create(Langchain4jAjMcpClientInfo info) {
		String key = info.getKey();
		if (key == null || key.trim().isEmpty()) {
			return false;
		}
		try {
			destroy(key);
			McpClient client = buildClient(info);
			client.initialize();
			infos.put(key, info);
			clients.put(key, client);
			providers.put(key, wrapWithAuthRetry(newAjMcpToolProvider(client, info), key, info));
			logger.info("注册 LangChain4j aj-mcp 客户端: {} -> {}", key, info.getTransport());
			return true;
		} catch (Throwable e) {
			logger.warn("构建 LangChain4j aj-mcp 客户端失败: key={}", key, e);
			return false;
		}
	}

	/** 按传输类型构建 aj-mcp 客户端。 */
	private McpClient buildClient(Langchain4jAjMcpClientInfo info) {
		String type = info.getTransport() == null || info.getTransport().trim().isEmpty()
				? "http-sse" : info.getTransport().trim();
		Map<String, String> headers = info.getCustomHeaders();
		McpTransport transport;
		if ("streamable-http".equals(type)) {
			if (info.getUrl() == null || info.getUrl().trim().isEmpty()) {
				throw new IllegalArgumentException("streamable-http 传输必须配置 url");
			}
			StreamableHttpTransport.StreamableHttpTransportBuilder transportBuilder =
					StreamableHttpTransport.builder().endpointUrl(info.getUrl());
			if (headers != null && !headers.isEmpty()) {
				transportBuilder.requestHeaders(headers);
			}
			transport = transportBuilder.build();
		} else if ("stdio".equals(type)) {
			if (info.getCommand() == null || info.getCommand().isEmpty()) {
				throw new IllegalArgumentException("stdio 传输必须配置 command");
			}
			StdioTransport.StdioTransportBuilder transportBuilder = StdioTransport.builder()
					.command(info.getCommand());
			if (info.getEnvironment() != null && !info.getEnvironment().isEmpty()) {
				transportBuilder.environment(info.getEnvironment());
			}
			transport = transportBuilder.build();
		} else if ("http-sse".equals(type)) {
			if (info.getUrl() == null || info.getUrl().trim().isEmpty()) {
				throw new IllegalArgumentException("http-sse 传输必须配置 url");
			}
			// 旧版 SSE 传输的 headers 只能走 5 参构造器（builder 未暴露）
			transport = new HttpMcpTransport(info.getUrl(), false, false, null, headers);
		} else {
			throw new IllegalArgumentException(String.format(
					"不支持的传输类型 '%s'（可选 http-sse / streamable-http / stdio）", type));
		}
		return McpClient.builder().transport(transport).build();
	}

	/**
	 * 把 aj-mcp 客户端的工具适配成 LangChain4j {@link ToolProvider}（0.35 API）：
	 * {@code provideTools} 时实时 {@code listTools}，工具执行转发
	 * {@code client.callTool(name, argumentsJson)}。
	 */
	private ToolProvider newAjMcpToolProvider(final McpClient client, final Langchain4jAjMcpClientInfo info) {
		final List<String> filter = info.getFilterToolNames() == null
				? Collections.<String>emptyList() : info.getFilterToolNames();
		return new ToolProvider() {
			@Override
			public ToolProviderResult provideTools(ToolProviderRequest request) {
				ToolProviderResult.Builder result = ToolProviderResult.builder();
				for (ToolItem item : client.listTools()) {
					if (!filter.isEmpty() && !filter.contains(item.getName())) {
						continue;
					}
					ToolSpecification.Builder spec = ToolSpecification.builder()
							.name(item.getName())
							.description(item.getDescription());
					spec.parameters(toJsonObjectSchema(item.getInputSchema()));
					result.add(spec.build(), new ToolExecutorAdapter(client));
				}
				return result.build();
			}
		};
	}

	/** MCP inputSchema → 1.20.2 JsonObjectSchema（properties 的 type/description + required）。 */
	private JsonObjectSchema toJsonObjectSchema(com.ajaxjs.mcp.protocol.tools.JsonSchema schema) {
		JsonObjectSchema.Builder params = JsonObjectSchema.builder();
		if (schema == null) {
			return params.build();
		}
		Map<String, com.fasterxml.jackson.databind.JsonNode> keywords = schema.getKeywords();
		if (keywords == null) {
			return params.build();
		}
		com.fasterxml.jackson.databind.JsonNode properties = keywords.get("properties");
		if (properties != null && properties.isObject()) {
			// properties 节点形如 {"参数名": {"type": "string", "description": "..."}}
			for (java.util.Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields =
					properties.fields(); fields.hasNext();) {
				Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
				String name = entry.getKey();
				com.fasterxml.jackson.databind.JsonNode node = entry.getValue();
				String type = node.hasNonNull("type") ? node.get("type").asText() : "string";
				String description = node.hasNonNull("description") ? node.get("description").asText() : null;
				switch (type) {
					case "integer":
						params.addIntegerProperty(name, description);
						break;
					case "number":
						params.addNumberProperty(name, description);
						break;
					case "boolean":
						params.addBooleanProperty(name, description);
						break;
					default:
						params.addStringProperty(name, description);
						break;
				}
			}
		}
		com.fasterxml.jackson.databind.JsonNode required = keywords.get("required");
		if (required != null && required.isArray()) {
			List<String> names = new ArrayList<String>();
			for (com.fasterxml.jackson.databind.JsonNode name : required) {
				names.add(name.asText());
			}
			params.required(names);
		}
		return params.build();
	}

	/** 工具执行适配：请求里的 arguments JSON 原样转发给 aj-mcp 客户端。 */
	private static class ToolExecutorAdapter implements dev.langchain4j.service.tool.ToolExecutor {
		private final McpClient client;

		ToolExecutorAdapter(McpClient client) {
			this.client = client;
		}

		@Override
		public String execute(dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest, Object memoryId) {
			return client.callTool(toolRequest.name(), toolRequest.arguments());
		}
	}

	/**
	 * 认证失败自动重试包装：工具执行遇到认证类异常（api-key 过期等）时，作废缓存并按
	 * {@code auth-retry-count} 重试；非认证异常直接抛出。
	 */
	private ToolProvider wrapWithAuthRetry(ToolProvider inner, final String key,
			final Langchain4jAjMcpClientInfo info) {
		final int retries = info.getAuthRetryCount() == null ? 0
				: Math.max(0, info.getAuthRetryCount());
		if (retries <= 0) {
			return inner;
		}
		return new ToolProvider() {
			@Override
			public ToolProviderResult provideTools(ToolProviderRequest request) {
				ToolProviderResult result = inner.provideTools(request);
				if (result == null || result.tools() == null || result.tools().isEmpty()) {
					return result;
				}
				ToolProviderResult.Builder wrapped = ToolProviderResult.builder();
				for (final Map.Entry<dev.langchain4j.agent.tool.ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> entry
						: result.tools().entrySet()) {
					final dev.langchain4j.service.tool.ToolExecutor executor = entry.getValue();
					wrapped.add(entry.getKey(), new dev.langchain4j.service.tool.ToolExecutor() {
						@Override
						public String execute(dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest,
								Object memoryId) {
							RuntimeException last = null;
							for (int attempt = 0; attempt <= retries; attempt++) {
								try {
									return executor.execute(toolRequest, memoryId);
								} catch (RuntimeException e) {
									if (!isAuthError(e)) {
										throw e;
									}
									last = e;
									logger.warn("aj-mcp 工具 {} 认证失败（第 {}/{} 次尝试）",
											entry.getKey().name(), attempt + 1, retries + 1);
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
						}
					});
				}
				return wrapped.build();
			}
		};
	}

	/** api-key 过期的错误特征：命中即触发重试 */
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

	/** 按 key 销毁实例（关闭底层 aj-mcp 客户端，释放 SSE/子进程）。 */
	public void destroy(String key) {
		if (key == null) {
			return;
		}
		providers.remove(key);
		infos.remove(key);
		McpClient client = clients.remove(key);
		if (client != null) {
			try {
				client.close();
			} catch (Exception e) {
				logger.warn("关闭 aj-mcp 客户端失败: key={}", key, e);
			}
		}
	}

	/** 按 key 取 ToolProvider，不存在时返回 {@code null}。 */
	public ToolProvider get(String key) {
		return key == null ? null : providers.get(key);
	}

	/**
	 * 手动刷新：销毁并按最近一次资源定义重建指定实例——重新连接 MCP Server、重新取工具列表。
	 *
	 * @return 刷新后的 ToolProvider
	 */
	public ToolProvider refresh(String key) {
		Langchain4jAjMcpClientInfo info = key == null ? null : infos.get(key);
		if (info == null) {
			throw new IllegalArgumentException(String.format(
					"未配置 LangChain4j aj-mcp 客户端: %s，无法刷新", key));
		}
		destroy(key);
		if (!create(info)) {
			throw new IllegalStateException(String.format("刷新 LangChain4j aj-mcp 客户端失败: %s", key));
		}
		return providers.get(key);
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
