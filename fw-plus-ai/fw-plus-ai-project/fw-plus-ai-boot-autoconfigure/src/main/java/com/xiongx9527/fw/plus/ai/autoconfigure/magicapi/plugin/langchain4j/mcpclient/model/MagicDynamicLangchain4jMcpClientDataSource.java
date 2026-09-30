package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model.Langchain4jMcpClientInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
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
			DefaultMcpClient client = DefaultMcpClient.builder()
					.clientName(info.getClientName() == null || info.getClientName().trim().isEmpty()
							? DEFAULT_CLIENT_NAME : info.getClientName())
					.transport(transport)
					.build();
			McpToolProvider.Builder builder = McpToolProvider.builder()
					.mcpClients(Collections.singletonList(client))
					.failIfOneServerFails(info.getFailIfOneServerFails() != null
							&& info.getFailIfOneServerFails());
			if (info.getFilterToolNames() != null && !info.getFilterToolNames().isEmpty()) {
				builder.filterToolNames(info.getFilterToolNames());
			}
			clients.put(key, client);
			providers.put(key, builder.build());
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
					// 每次请求动态取头（如调 token 接口换 api-key）；函数失败回落静态头
					String functionName = info.getCustomHeadersFunction().trim();
					Map<String, String> staticHeaders = hasStaticHeaders ? info.getCustomHeaders() : null;
					httpBuilder.customHeaders((java.util.function.Supplier<Map<String, String>>) () ->
							resolveHeaders(functionName, staticHeaders));
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

	/**
	 * 解析动态请求头：调 magic-api function 取最新头（如换新 api-key），
	 * 与静态头合并（动态覆盖同名 key）；函数失败时回落静态头并告警。
	 */
	private Map<String, String> resolveHeaders(String functionName, Map<String, String> staticHeaders) {
		Map<String, String> merged = new java.util.HashMap<>();
		if (staticHeaders != null) {
			merged.putAll(staticHeaders);
		}
		try {
			Object result = invoker.invokeFunction(functionName, Collections.<String, Object>emptyMap());
			if (result instanceof Map) {
				for (Object entry : ((Map<?, ?>) result).entrySet()) {
					Object k = ((Map.Entry<?, ?>) entry).getKey();
					Object v = ((Map.Entry<?, ?>) entry).getValue();
					if (k != null && v != null) {
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
		return merged;
	}

	/** 按 key 销毁实例（关闭底层客户端，释放连接/子进程/容器）。 */
	public void destroy(String key) {
		if (key == null) {
			return;
		}
		providers.remove(key);
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
