package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpInstanceProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.mcp.server.McpAsyncServerImpl;
import com.xiongx9527.fw.plus.ai.mcp.server.McpServerBuilder;
import com.xiongx9527.fw.plus.ai.mcp.server.McpServerCapabilities;
import com.xiongx9527.fw.plus.ai.mcp.server.McpServerInfo;
import com.xiongx9527.fw.plus.ai.mcp.server.MethodMcpTool;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPageRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceTemplate;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpTool;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolProvider;
import com.xiongx9527.fw.plus.ai.mcp.transport.McpStreamableTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * MCP Server 基础自动配置
 *
 * <p>spec 8：注册 McpProperties；为每个 enabled 实例准备 server 上下文，
 * 注入所有 {@code McpToolProvider}/{@code McpResourceProvider}/
 * {@code McpPromptProvider}/{@code McpCompletionProvider} Bean，并扫描 @McpTool 注解方法。
 *
 * <p>不在此处注册任何 Web 栈 bean（controller/transport）- 由
 * {@code McpWebMvcAutoConfiguration} 与 {@code McpWebFluxAutoConfiguration}
 * 按 Web 应用类型互斥装配。
 *
 * <p>对齐 spec 7.2：@McpTool 注解便捷模式仅适用于 Tool；Resource/Prompt 只走 SPI。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
@Configuration
@Import({McpWebMvcAutoConfiguration.class, McpWebFluxAutoConfiguration.class})
@ConditionalOnClass(McpAsyncServerImpl.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(McpProperties.class)
public class McpAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(McpAutoConfiguration.class);

	/**
	 * 创建 MCP Server 实例注册表
	 *
	 * <p>对齐 spec 8.2：未配置 instances 时提供一个默认实例（端点 /mcp、默认 serverInfo）。
	 *
	 * <p>本 Bean 不构建也不启动 server；真正的 build/start 由 Web 栈
	 * autoconfiguration 调用 {@link McpServerInstanceRegistry#bindAndStart} 完成。
	 */
	@Bean(destroyMethod = "stopAll")
	public McpServerInstanceRegistry mcpServerInstanceRegistry(
			ApplicationContext applicationContext,
			McpProperties properties,
			List<McpToolProvider> toolProviders,
			List<McpResourceProvider> resourceProviders,
			List<McpPromptProvider> promptProviders,
			List<McpCompletionProvider> completionProviders) {

		List<McpInstanceProperties> instances = properties.getInstances();
		if (instances == null || instances.isEmpty()) {
			instances = new ArrayList<>();
			instances.add(new McpInstanceProperties());
		}

		List<McpInstanceProperties> enabledConfigs = new ArrayList<>();
		for (McpInstanceProperties config : instances) {
			if (config.isEnabled()) {
				enabledConfigs.add(config);
			} else {
				logger.info("MCP instance '{}' is disabled, skipping", config.getName());
			}
		}

		List<McpToolProvider> effectiveToolProviders = new ArrayList<>(toolProviders);
		List<MethodMcpTool> methodTools = scanMcpToolAnnotatedBeans(applicationContext);

		return new McpServerInstanceRegistry(
				enabledConfigs,
				effectiveToolProviders,
				resourceProviders,
				promptProviders,
				completionProviders,
				methodTools);
	}

	/**
	 * 扫描所有 Spring Bean 中标注 @McpTool 的 public 方法
	 *
	 * <p>spec 7.2：返回每个方法对应的 {@link MethodMcpTool} 实例（包装为 {@link McpToolProvider}）。
	 */
	private List<MethodMcpTool> scanMcpToolAnnotatedBeans(ApplicationContext context) {
		List<MethodMcpTool> methodTools = new ArrayList<>();
		String[] beanNames = context.getBeanDefinitionNames();
		for (String beanName : beanNames) {
			Class<?> beanType;
			try {
				beanType = context.getType(beanName);
			} catch (Exception e) {
				continue;
			}
			if (beanType == null) {
				continue;
			}
			String typeName = beanType.getName();
			if (typeName.startsWith("org.springframework")
					|| typeName.startsWith("com.xiongx9527.fw.plus.ai.autoconfigure")
					|| typeName.startsWith("com.xiongx9527.fw.plus.ai.mcp.")) {
				continue;
			}
			Object bean;
			try {
				bean = context.getBean(beanName);
			} catch (Exception e) {
				continue;
			}
			for (Method method : beanType.getMethods()) {
				com.xiongx9527.fw.plus.ai.mcp.annotation.McpTool annotation = method
						.getAnnotation(com.xiongx9527.fw.plus.ai.mcp.annotation.McpTool.class);
				if (annotation == null) {
					continue;
				}
				McpTool toolDefinition = McpTool.builder()
						.name(annotation.name())
						.description(annotation.description())
						.title(annotation.title())
						.icon(annotation.icon())
						.build();
				try {
					MethodMcpTool methodTool = new MethodMcpTool(bean, method, toolDefinition);
					methodTools.add(methodTool);
					logger.info("Registered @McpTool '{}' from bean {}", annotation.name(), beanName);
				} catch (Exception e) {
					logger.error("Failed to register @McpTool '{}' from bean {}: {}",
							annotation.name(), beanName, e.getMessage(), e);
				}
			}
		}
		return methodTools;
	}

	/**
	 * MCP Server 实例注册表
	 *
	 * <p>持有所有 enabled 实例配置 + SPI 提供者；由 Web 栈 autoconfiguration
	 * 调用 {@link #bindAndStart} 注入 transport 后再构建并启动 server。
	 */
	public static class McpServerInstanceRegistry {

		private final Map<String, McpAsyncServerImpl> servers = new LinkedHashMap<>();
		private final Map<String, McpInstanceProperties> configs = new LinkedHashMap<>();
		private final Map<String, McpStreamableTransport> transports = new LinkedHashMap<>();
		private final Map<String, McpStreamableTransport> transportsByPath = new LinkedHashMap<>();
		private final List<McpToolProvider> toolProviders;
		private final List<McpResourceProvider> resourceProviders;
		private final List<McpPromptProvider> promptProviders;
		private final List<McpCompletionProvider> completionProviders;
		private final List<MethodMcpTool> methodTools;
		private volatile boolean bound = false;

		public McpServerInstanceRegistry(
				List<McpInstanceProperties> configs,
				List<McpToolProvider> toolProviders,
				List<McpResourceProvider> resourceProviders,
				List<McpPromptProvider> promptProviders,
				List<McpCompletionProvider> completionProviders,
				List<MethodMcpTool> methodTools) {
			for (McpInstanceProperties config : configs) {
				this.configs.put(config.getName(), config);
			}
			validateUniquePaths();
			this.toolProviders = toolProviders != null ? toolProviders : Collections.<McpToolProvider>emptyList();
			this.resourceProviders = resourceProviders != null
					? resourceProviders : Collections.<McpResourceProvider>emptyList();
			this.promptProviders = promptProviders != null
					? promptProviders : Collections.<McpPromptProvider>emptyList();
			this.completionProviders = completionProviders != null
					? completionProviders : Collections.<McpCompletionProvider>emptyList();
			this.methodTools = methodTools != null ? methodTools : Collections.<MethodMcpTool>emptyList();
		}

		/**
		 * 由 Web 栈 autoconfiguration 调用：注入 transport 后构建并启动所有实例
		 *
		 * <p>每个实例需要自己的 transport（独立 SSE 会话空间）。
		 *
		 * @param transportFactory 接受实例配置，返回该实例专属的
		 *        {@link McpStreamableTransport}（独立端点路径与独立 SSE 会话空间）
		 */
		public synchronized void bindAndStart(Function<McpInstanceProperties, McpStreamableTransport> transportFactory) {
			if (bound) {
				return;
			}
			McpProviderContext emptyContext = McpProviderContext.context();
			McpPageRequest pageRequest = new McpPageRequest();
			for (McpInstanceProperties config : configs.values()) {
				try {
					McpStreamableTransport transport = transportFactory.apply(config);
					if (transport == null) {
						logger.warn("No transport created for instance {}, skipping", config.getName());
						continue;
					}
					McpServerBuilder builder = new McpServerBuilder(transport);
					if (config.getDescription() != null && !config.getDescription().isEmpty()) {
						builder.serverInfo(new McpServerInfo(
								config.getServerName(), config.getServerVersion(), config.getDescription()));
					} else {
						builder.serverInfo(config.getServerName(), config.getServerVersion());
					}
					if (config.getInstructions() != null && !config.getInstructions().isEmpty()) {
						builder.instructions(config.getInstructions());
					}
					builder.capabilities(McpServerCapabilities.builder()
							.tools(config.isToolsEnabled())
							.resources(config.isResourcesEnabled())
							.prompts(config.isPromptsEnabled())
							.completions(config.isCompletionsEnabled())
							.logging(config.isLoggingEnabled())
							.build());
					// 注册 SPI Tool 提供者：展开 provider.listTools 为每个 tool 调用 builder.tool
					if (config.isToolsEnabled()) {
						for (McpToolProvider provider : toolProviders) {
							List<McpTool> tools = provider.listTools(emptyContext, pageRequest);
							if (tools == null) {
								continue;
							}
							for (McpTool tool : tools) {
								if (tool == null) {
									continue;
								}
								builder.tool(tool, provider);
							}
						}
						// 注册 @McpTool 注解方法
						for (MethodMcpTool methodTool : methodTools) {
							builder.tool(methodTool.toolDefinition(), methodTool);
						}
					}
					// 注册 SPI Resource 提供者
					if (config.isResourcesEnabled()) {
						for (McpResourceProvider provider : resourceProviders) {
							List<McpResourceInfo> resources = provider.listResources(emptyContext);
							if (resources != null) {
								for (McpResourceInfo resource : resources) {
									if (resource != null) {
										builder.resource(resource, provider);
									}
								}
							}
							List<McpResourceTemplate> templates = provider.listTemplates(emptyContext);
							if (templates != null) {
								for (McpResourceTemplate template : templates) {
									if (template != null) {
										builder.resourceTemplate(template, provider);
									}
								}
							}
						}
					}
					// 注册 SPI Prompt 提供者
					if (config.isPromptsEnabled()) {
						for (McpPromptProvider provider : promptProviders) {
							List<McpPromptInfo> prompts = provider.listPrompts(emptyContext);
							if (prompts == null) {
								continue;
							}
							for (McpPromptInfo prompt : prompts) {
								if (prompt == null) {
									continue;
								}
								builder.prompt(prompt, provider);
							}
						}
					}
					// 注册 Completion 提供者
					if (config.isCompletionsEnabled()) {
						for (McpCompletionProvider provider : completionProviders) {
							builder.completion(provider);
						}
					}
					McpAsyncServerImpl server = builder.build();
					transport.bindServer(server);
					server.start();
					servers.put(config.getName(), server);
					transports.put(config.getName(), transport);
					transportsByPath.put(transport.getPath(), transport);
					logger.info("MCP instance '{}' bound and started at path {}",
							config.getName(), transport.getPath());
				} catch (Exception e) {
					logger.error("Failed to bind MCP instance {}", config.getName(), e);
				}
			}
			bound = true;
		}

		public McpAsyncServerImpl getServer(String name) {
			return servers.get(name);
		}

		/**
		 * 返回首个实例的 server；单实例场景下 controller 使用。
		 */
		public McpAsyncServerImpl getFirstServer() {
			if (servers.isEmpty()) {
				return null;
			}
			return servers.values().iterator().next();
		}

		public Map<String, McpAsyncServerImpl> getAllServers() {
			return Collections.unmodifiableMap(servers);
		}

		public Map<String, McpInstanceProperties> getAllConfigs() {
			return Collections.unmodifiableMap(configs);
		}

		public McpInstanceProperties getFirstConfig() {
			if (configs.isEmpty()) {
				return null;
			}
			return configs.values().iterator().next();
		}

		/**
		 * 按实例名称取 transport
		 */
		public McpStreamableTransport getTransport(String name) {
			return transports.get(name);
		}

		/**
		 * 按端点路径取 transport（Web 端点按请求路径路由到实例）
		 */
		public McpStreamableTransport getTransportByPath(String path) {
			if (path == null) {
				return null;
			}
			return transportsByPath.get(normalizePath(path));
		}

		/**
		 * 已绑定的全部 transport（按实例名称）
		 */
		public Map<String, McpStreamableTransport> getAllTransports() {
			return Collections.unmodifiableMap(transports);
		}

		public void stopAll() {
			for (McpStreamableTransport transport : transports.values()) {
				try {
					transport.stop();
				} catch (Exception e) {
					logger.error("Error stopping MCP transport", e);
				}
			}
			for (McpAsyncServerImpl server : servers.values()) {
				try {
					server.stop();
				} catch (Exception e) {
					logger.error("Error stopping MCP server", e);
				}
			}
			servers.clear();
			transports.clear();
			transportsByPath.clear();
		}

		/**
		 * 校验实例路径唯一（多实例共享同一路径会使端点映射注册失败）
		 */
		private void validateUniquePaths() {
			Map<String, String> seen = new LinkedHashMap<>();
			for (McpInstanceProperties config : configs.values()) {
				String path = normalizePath(config.getPath());
				String previous = seen.put(path, config.getName());
				if (previous != null) {
					throw new IllegalStateException("Duplicated MCP endpoint path '" + path
							+ "' configured by instances '" + previous + "' and '" + config.getName() + "'");
				}
			}
		}

		private static String normalizePath(String path) {
			String normalized = path;
			if (normalized == null || normalized.trim().isEmpty()) {
				normalized = "/mcp";
			}
			normalized = normalized.trim();
			if (!normalized.startsWith("/")) {
				normalized = "/" + normalized;
			}
			while (normalized.length() > 1 && normalized.endsWith("/")) {
				normalized = normalized.substring(0, normalized.length() - 1);
			}
			return normalized;
		}
	}
}
