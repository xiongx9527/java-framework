package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter;

import com.ajaxjs.mcp.server.McpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpRuntimeFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.webflux.McpFluxRouteRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.webmvc.McpRoutingRegistrar;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicDynamicMcpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicMcpPromptInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicMcpPromptMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicApiMcpCapabilityRegistrar;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicMcpCapabilitySource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.spi.MagicApiMcpCapabilityProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicMcpResourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicMcpResourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.service.MagicMcpServerInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.service.MagicMcpServerMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.web.MagicMcpDiagnosticController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.web.MagicMcpServerController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpScriptInvoker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolChangeListener;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolInvoker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolRegistrar;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolRegistry;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@Import(MagicMcpClusterConfiguration.class)
@ConditionalOnClass({McpServer.class, McpServerRuntime.class})
@ConditionalOnBean(MagicResourceService.class)
@ConditionalOnProperty(prefix = "magic-api.plugin.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MagicApiMcpProperties.class)
public class MagicAPIMcpConfiguration implements MagicPluginConfiguration {

    private final MagicApiMcpProperties properties;

    public MagicAPIMcpConfiguration(MagicApiMcpProperties properties) {
        this.properties = properties;
    }

    @Override
    public Plugin plugin() {
        return new Plugin("mcp", "MagicMcp", "magic-mcp.1.0.0.iife.js");
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping
                .registerController(new MagicMcpServerController(configuration))
                .registerController(new MagicMcpDiagnosticController(configuration));
    }


    /**
     * 工具注册表：以 Magic-API 资源为数据源，实时快照。
     */
    @Bean
    public MagicApiMcpToolRegistry magicApiMcpToolRegistry(MagicResourceService magicResourceService,
            ObjectProvider<List<MagicApiMcpCapabilityProvider>> capabilityProviderProvider) {
        MagicApiMcpToolRegistry registry = new MagicApiMcpToolRegistry(magicResourceService, properties);
        List<MagicApiMcpCapabilityProvider> providers = capabilityProviderProvider.getIfAvailable();
        if (providers != null) {
            registry.setCapabilityProviders(providers);
        }
        return registry;
    }

    /**
     * tools/call 执行器：function 走 magic.invoke，api 走脚本执行。
     */
    @Bean
    public MagicApiMcpToolInvoker magicApiMcpToolInvoker(MagicApiMcpToolRegistry toolRegistry) {
        MagicApiMcpScriptInvoker scriptInvoker = new MagicApiMcpScriptInvoker();
        return new MagicApiMcpToolInvoker(toolRegistry, scriptInvoker, scriptInvoker);
    }

    /**
     * server 工厂：创建接管 tools/call 的 {@link MagicApiMcpServerRegistry}。
     */
    @Bean
    public McpServerFactory magicApiMcpServerFactory(MagicApiMcpToolInvoker toolInvoker) {
        return (config, featureMgr) -> new MagicApiMcpServerRegistry(toolInvoker);
    }

    /**
     * 启动后把工具快照注入各 runtime 的 toolStore，并通知客户端工具列表变更。
     */
    @Bean
    public MagicApiMcpToolRegistrar magicApiMcpToolRegistrar(MagicApiMcpToolRegistry toolRegistry,
            ObjectProvider<List<McpServerRuntime>> runtimesProvider) {
        MagicApiMcpToolRegistrar registrar = new MagicApiMcpToolRegistrar(toolRegistry);
        List<McpServerRuntime> runtimes = runtimesProvider.getIfAvailable();
        if (runtimes != null) {
            for (McpServerRuntime runtime : runtimes) {
                registrar.register(runtime.getFeatureMgr(), runtime.getServer());
            }
        }
        return registrar;
    }

    /**
     * mcp-resource 资源类型存储。
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicMcpResourceInfoMagicResourceStorage magicMcpResourceInfoMagicResourceStorage() {
        return new MagicMcpResourceInfoMagicResourceStorage();
    }

    /**
     * mcp-prompt 资源类型存储。
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicMcpPromptInfoMagicResourceStorage magicMcpPromptInfoMagicResourceStorage() {
        return new MagicMcpPromptInfoMagicResourceStorage();
    }

    /**
     * mcp-resource 动态注册表。
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicMcpResourceMagicDynamicRegistry magicMcpResourceMagicDynamicRegistry(
            MagicMcpResourceInfoMagicResourceStorage storage, MagicMcpNotifier notifier,
            MagicApiMcpCapabilityRegistrar capabilityRegistrar) {
        return new MagicMcpResourceMagicDynamicRegistry(storage, notifier, capabilityRegistrar);
    }

    /**
     * mcp-prompt 动态注册表。
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicMcpPromptMagicDynamicRegistry magicMcpPromptMagicDynamicRegistry(
            MagicMcpPromptInfoMagicResourceStorage storage, MagicMcpNotifier notifier,
            MagicApiMcpCapabilityRegistrar capabilityRegistrar) {
        return new MagicMcpPromptMagicDynamicRegistry(storage, notifier, capabilityRegistrar);
    }

    /**
     * 资源变更后增量刷新工具快照。
     */
    @Bean
    public MagicApiMcpToolChangeListener magicApiMcpToolChangeListener(MagicApiMcpToolRegistrar registrar,
            ObjectProvider<List<McpServerRuntime>> runtimesProvider, MagicMcpNotifier notifier,
            ObjectProvider<MagicDynamicMcpServer> dataSourceProvider) {
        return new MagicApiMcpToolChangeListener(registrar, runtimesProvider.getIfAvailable(),
                dataSourceProvider.getIfAvailable(), notifier);
    }

    /**
     * resources/prompts 能力注册器：把两类资源快照同步进各实例的 store。
     */
    @Bean
    public MagicApiMcpCapabilityRegistrar magicApiMcpCapabilityRegistrar(
            ObjectProvider<List<McpServerRuntime>> runtimesProvider,
            ObjectProvider<MagicDynamicMcpServer> dataSourceProvider,
            ObjectProvider<MagicMcpResourceMagicDynamicRegistry> resourceRegistryProvider,
            ObjectProvider<MagicMcpPromptMagicDynamicRegistry> promptRegistryProvider,
            ObjectProvider<List<MagicApiMcpCapabilityProvider>> capabilityProviderProvider) {
        return new MagicApiMcpCapabilityRegistrar(new MagicMcpCapabilitySource(runtimesProvider, dataSourceProvider,
                resourceRegistryProvider, promptRegistryProvider), capabilityProviderProvider.getIfAvailable());
    }

    /**
     * 能力变更通知：api/function 走 tools、mcp-resource 走 resources、mcp-prompt 走 prompts。
     */
    @Bean
    public MagicMcpNotifier magicMcpNotifier(ObjectProvider<List<McpServerRuntime>> runtimesProvider,
            ObjectProvider<MagicDynamicMcpServer> dataSourceProvider,
            ObjectProvider<MagicApiMcpToolRegistrar> toolRegistrarProvider,
            ObjectProvider<MagicApiMcpCapabilityRegistrar> capabilityRegistrarProvider,
            ApplicationEventPublisher eventPublisher) {
        return new MagicMcpNotifier(runtimesProvider.getIfAvailable(), dataSourceProvider.getIfAvailable(),
                toolRegistrarProvider, capabilityRegistrarProvider, eventPublisher);
    }

    /**
     * mcp-server 资源类型存储。
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicMcpServerInfoMagicResourceStorage magicMcpServerInfoMagicResourceStorage() {
        return new MagicMcpServerInfoMagicResourceStorage();
    }

    /**
     * MCP server 动态数据源：按资源定义创建/销毁独立 server 实例。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(McpRuntimeFactory.class)
    public MagicDynamicMcpServer magicDynamicMcpServer(McpRuntimeFactory runtimeFactory,
            McpAuthService authService, ObjectProvider<McpRedisBroadcaster> broadcasterProvider,
            MagicResourceService magicResourceService, MagicApiMcpToolInvoker toolInvoker,
            ObjectProvider<MagicDynamicMcpServer.RuntimeRouteBinder> routeBinderProvider,
            ObjectProvider<MagicApiMcpCapabilityRegistrar> capabilityRegistrarProvider) {
        return new MagicDynamicMcpServer(properties, magicResourceService, runtimeFactory, authService,
                broadcasterProvider == null ? null : broadcasterProvider.getIfAvailable(), toolInvoker,
                routeBinderProvider.getIfAvailable(), capabilityRegistrarProvider);
    }

    /**
     * mcp-server 动态注册表：资源变更即增删 server 实例。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(McpRuntimeFactory.class)
    public MagicMcpServerMagicDynamicRegistry magicMcpServerMagicDynamicRegistry(
            MagicMcpServerInfoMagicResourceStorage storage, MagicDynamicMcpServer dataSource) {
        return new MagicMcpServerMagicDynamicRegistry(storage, dataSource, properties);
    }

    /**
     * webflux 路由绑定适配：把运行期新增/移除的 runtime 反映到路由委托。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(McpFluxRouteRegistry.class)
    public MagicDynamicMcpServer.RuntimeRouteBinder magicApiMcpFluxRouteBinder(McpFluxRouteRegistry registry) {
        return registry;
    }

    /**
     * web 路由绑定适配：把运行期新增/移除的 runtime 注册/注销到 handlerMapping。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(McpRoutingRegistrar.class)
    public MagicDynamicMcpServer.RuntimeRouteBinder magicApiMcpRouteBinder(McpRoutingRegistrar routingRegistrar) {
        return new MagicDynamicMcpServer.RuntimeRouteBinder() {
            @Override
            public void bind(McpServerRuntime runtime) {
                routingRegistrar.registerRuntime(runtime);
            }

            @Override
            public void unbind(McpServerRuntime runtime) {
                routingRegistrar.unregisterRuntime(runtime);
            }
        };
    }
}
