package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAccessTokenVerifier;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.SaTokenMcpAccessTokenVerifier;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.SaTokenMcpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.client.McpClientRequestService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.completion.McpCompletionHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpLifecycleHandlers;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.logging.McpLogService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.logging.McpLoggingHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification.MagicNotifyMcpChangeEventPublisher;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification.McpChangeEventPublisher;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification.McpClusterChangeEventListener;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification.McpNotificationService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification.McpToolMagicChangeListener;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpMethodHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.prompts.McpPromptsHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.MagicApiMcpPromptProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.MagicApiMcpResourceProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.McpCompletionProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.McpPromptProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.McpResourceProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.McpToolProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.resources.McpResourceSubscriptionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.resources.McpResourcesHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.InMemoryMcpSessionRepository;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionRepository;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service.McpPromptMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service.McpPromptMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service.McpResourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service.McpResourceMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.tools.McpToolsHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.MagicMcpController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.McpHttpTransportService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.McpSseStreamService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicNotifyService;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(MagicAPIMcpProperties.class)
@ConditionalOnProperty(prefix = "magic-api.plugin.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MagicAPIMcpConfiguration implements MagicPluginConfiguration {

	private final ObjectProvider<MagicMcpController> magicMcpControllerProvider;

	public MagicAPIMcpConfiguration(ObjectProvider<MagicMcpController> magicMcpControllerProvider) {
		this.magicMcpControllerProvider = magicMcpControllerProvider;
	}

	@Bean
	@ConditionalOnMissingBean
	public McpSessionRepository mcpSessionRepository() {
		return new InMemoryMcpSessionRepository();
	}

	@Bean
	public McpSessionService mcpSessionService(McpSessionRepository sessionRepository, MagicAPIMcpProperties properties) {
		return new McpSessionService(sessionRepository, properties.getSessionTimeoutSeconds());
	}

	@Bean
	public McpLifecycleHandlers mcpLifecycleHandlers(McpSessionService mcpSessionService,
										   MagicAPIMcpProperties properties,
										   List<McpCompletionProvider> completionProviders) {
		McpLifecycleHandlers.Capabilities capabilities = new McpLifecycleHandlers.Capabilities();
		capabilities.setToolsListChanged(true);
		capabilities.setResourcesListChanged(true);
		capabilities.setResourceSubscribe(true);
		capabilities.setPromptsListChanged(true);
		capabilities.setLogging(true);
		capabilities.setCompletion(completionProviders != null && !completionProviders.isEmpty());
		return new McpLifecycleHandlers(mcpSessionService, properties, capabilities);
	}

	@Bean
	public McpToolsHandler mcpToolsHandler(List<McpToolProvider> toolProviders,
										   SaTokenMcpAuthService authService,
										   MagicAPIMcpProperties properties) {
		return new McpToolsHandler(toolProviders, authService, properties);
	}

	@Bean
	@ConditionalOnBean(MagicResourceService.class)
	public MagicApiMcpToolProvider magicApiMcpToolProvider(MagicResourceService magicResourceService,
														   MagicAPIMcpProperties properties) {
		return new MagicApiMcpToolProvider(magicResourceService, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public McpResourceMagicResourceStorage mcpResourceMagicResourceStorage() {
		return new McpResourceMagicResourceStorage();
	}

	@Bean
	@ConditionalOnMissingBean
	public McpPromptMagicResourceStorage mcpPromptMagicResourceStorage() {
		return new McpPromptMagicResourceStorage();
	}

	@Bean
	@ConditionalOnMissingBean
	public McpResourceMagicDynamicRegistry mcpResourceMagicDynamicRegistry(
			McpResourceMagicResourceStorage mcpResourceMagicResourceStorage,
			McpNotificationService notificationService) {
		return new McpResourceMagicDynamicRegistry(mcpResourceMagicResourceStorage, notificationService);
	}

	@Bean
	@ConditionalOnMissingBean
	public McpPromptMagicDynamicRegistry mcpPromptMagicDynamicRegistry(
			McpPromptMagicResourceStorage mcpPromptMagicResourceStorage,
			McpNotificationService notificationService) {
		return new McpPromptMagicDynamicRegistry(mcpPromptMagicResourceStorage, notificationService);
	}

	@Bean
	@ConditionalOnBean(MagicResourceService.class)
	@ConditionalOnMissingBean(MagicApiMcpResourceProvider.class)
	public MagicApiMcpResourceProvider magicApiMcpResourceProvider(MagicResourceService magicResourceService) {
		return new MagicApiMcpResourceProvider(magicResourceService);
	}

	@Bean
	@ConditionalOnBean(MagicResourceService.class)
	@ConditionalOnMissingBean(MagicApiMcpPromptProvider.class)
	public MagicApiMcpPromptProvider magicApiMcpPromptProvider(MagicResourceService magicResourceService) {
		return new MagicApiMcpPromptProvider(magicResourceService);
	}

	@Bean
	public McpResourceSubscriptionService mcpResourceSubscriptionService() {
		return new McpResourceSubscriptionService();
	}

	@Bean
	public McpResourcesHandler mcpResourcesHandler(List<McpResourceProvider> resourceProviders,
												   McpResourceSubscriptionService subscriptionService,
												   SaTokenMcpAuthService authService,
												   MagicAPIMcpProperties properties) {
		return new McpResourcesHandler(resourceProviders, subscriptionService, authService, properties);
	}

	@Bean
	public McpPromptsHandler mcpPromptsHandler(List<McpPromptProvider> promptProviders,
											   SaTokenMcpAuthService authService,
											   MagicAPIMcpProperties properties) {
		return new McpPromptsHandler(promptProviders, authService, properties);
	}

	@Bean
	public McpCompletionHandler mcpCompletionHandler(List<McpCompletionProvider> completionProviders) {
		return new McpCompletionHandler(completionProviders);
	}

	@Bean
	public McpLogService mcpLogService() {
		return new McpLogService();
	}

	@Bean
	public McpLoggingHandler mcpLoggingHandler(McpLogService logService,
											   SaTokenMcpAuthService authService,
											   MagicAPIMcpProperties properties) {
		return new McpLoggingHandler(logService, authService, properties);
	}

	@Bean
	public McpClientRequestService mcpClientRequestService(McpSessionService sessionService) {
		return new McpClientRequestService(sessionService);
	}

	@Bean
	public McpNotificationService mcpNotificationService(McpSseStreamService sseStreamService,
														 McpResourceSubscriptionService subscriptionService,
														 ObjectProvider<McpChangeEventPublisher> changeEventPublisher,
														 McpSessionRepository sessionRepository) {
		return new McpNotificationService(sseStreamService, subscriptionService, changeEventPublisher.getIfAvailable(),
				sessionRepository);
	}

	@Bean
	@ConditionalOnProperty(prefix = "magic-api.plugin.cluster", name = "enabled", havingValue = "true")
	@ConditionalOnBean(MagicNotifyService.class)
	@ConditionalOnMissingBean
	public McpChangeEventPublisher mcpChangeEventPublisher(MagicNotifyService magicNotifyService,
														   MagicAPIProperties magicAPIProperties) {
		return new MagicNotifyMcpChangeEventPublisher(magicNotifyService, magicAPIProperties.getInstanceId());
	}

	@Bean
	@ConditionalOnProperty(prefix = "magic-api.plugin.cluster", name = "enabled", havingValue = "true")
	@ConditionalOnBean(McpChangeEventPublisher.class)
	public McpClusterChangeEventListener mcpClusterChangeEventListener(McpNotificationService notificationService) {
		return new McpClusterChangeEventListener(notificationService);
	}

	@Bean
	public McpToolMagicChangeListener mcpToolMagicChangeListener(McpNotificationService notificationService) {
		return new McpToolMagicChangeListener(notificationService);
	}

	@Bean
	public McpJsonRpcDispatcher mcpJsonRpcDispatcher(McpLifecycleHandlers lifecycleHandlers,
													 McpToolsHandler toolsHandler,
													 McpResourcesHandler resourcesHandler,
													 McpPromptsHandler promptsHandler,
													 McpCompletionHandler completionHandler,
													 McpLoggingHandler loggingHandler) {
		Map<String, McpMethodHandler> handlers = new HashMap<>();
		handlers.put("initialize", (context, params) -> lifecycleHandlers.initialize(context, asMap(params)));
		handlers.put("notifications/initialized", lifecycleHandlers::initialized);
		handlers.put("notifications/cancelled", (context, params) -> lifecycleHandlers.cancelled(context, asMap(params)));
		handlers.put("notifications/progress", (context, params) -> lifecycleHandlers.progress(context, asMap(params)));
		handlers.put("ping", lifecycleHandlers::ping);
		handlers.put("tools/list", (context, params) -> toolsHandler.listTools(context, asMap(params)));
		handlers.put("tools/call", (context, params) -> toolsHandler.callTool(context, asMap(params)));
		handlers.put("resources/list", (context, params) -> resourcesHandler.listResources(context, asMap(params)));
		handlers.put("resources/read", (context, params) -> resourcesHandler.readResource(context, asMap(params)));
		handlers.put("resources/templates/list", (context, params) -> resourcesHandler.listTemplates(context, asMap(params)));
		handlers.put("resources/subscribe", (context, params) -> resourcesHandler.subscribe(context, asMap(params)));
		handlers.put("resources/unsubscribe", (context, params) -> resourcesHandler.unsubscribe(context, asMap(params)));
		handlers.put("prompts/list", (context, params) -> promptsHandler.listPrompts(context, asMap(params)));
		handlers.put("prompts/get", (context, params) -> promptsHandler.getPrompt(context, asMap(params)));
		handlers.put("completion/complete", (context, params) -> completionHandler.complete(context, asMap(params)));
		handlers.put("logging/setLevel", (context, params) -> loggingHandler.setLevel(context, asMap(params)));
		return new McpJsonRpcDispatcher(handlers, lifecycleHandlers);
	}

	@Bean
	@ConditionalOnMissingBean
	public McpAccessTokenVerifier mcpAccessTokenVerifier() {
		return new SaTokenMcpAccessTokenVerifier();
	}

	@Bean
	public SaTokenMcpAuthService saTokenMcpAuthService(MagicAPIMcpProperties properties,
													   McpAccessTokenVerifier accessTokenVerifier) {
		return new SaTokenMcpAuthService(properties, accessTokenVerifier);
	}

	@Bean
	public McpHttpTransportService mcpHttpTransportService(MagicAPIMcpProperties properties,
														   McpJsonRpcDispatcher dispatcher,
														   McpSessionService sessionService,
														   SaTokenMcpAuthService authService,
														   McpSseStreamService sseStreamService) {
		return new McpHttpTransportService(properties, dispatcher, sessionService, authService, sseStreamService);
	}

	@Bean
	public McpSseStreamService mcpSseStreamService(MagicAPIMcpProperties properties,
										   SaTokenMcpAuthService authService,
										   McpSessionService sessionService) {
		return new McpSseStreamService(properties, authService, sessionService);
	}

	@Bean
	public MagicMcpController magicMcpController(MagicAPIMcpProperties properties,
												 McpHttpTransportService transportService,
												 McpSseStreamService sseStreamService) {
		return new MagicMcpController(properties, transportService, sseStreamService);
	}

	@Override
	public Plugin plugin() {
		return new Plugin("mcp", "MagicMcp", "magic-mcp.1.0.0.iife.js");
	}

	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> mapping.registerController(magicMcpControllerProvider.getObject());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object params) {
		return params instanceof Map ? (Map<String, Object>) params : null;
	}
}
