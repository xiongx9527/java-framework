package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web.Langchain4jDatasourceController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.web.MagicMcpDiagnosticController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.web.MagicMcpServerController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.MagicAPIWebFluxMapping;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.ssssssss.magicapi.core.config.MagicConfiguration;

/**
 * AI 层 magicapi 插件控制器的 WebFlux 注册器（对齐 Boot 层 {@code MagicAPIWebFluxControllerRegistrar}
 * 风格）：按插件功能 bean 存在性门控，把 AI 侧控制器注册进响应式 {@code RequestMappingHandlerMapping}。
 *
 * <p>servlet 栈下各插件经 {@code MagicPluginConfiguration.controllerRegister()} 由 fork 官方
 * starter 注册，本类只在 reactive 栈工作；两栈端点路径一致。</p>
 */
public class MagicAPIWebFluxPluginControllerRegistrar implements SmartInitializingSingleton {

	private final MagicAPIWebFluxMapping mapping;
	private final MagicConfiguration configuration;
	private final ApplicationContext applicationContext;

	public MagicAPIWebFluxPluginControllerRegistrar(MagicAPIWebFluxMapping mapping,
													MagicConfiguration configuration,
													ApplicationContext applicationContext) {
		this.mapping = mapping;
		this.configuration = configuration;
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterSingletonsInstantiated() {
		registerLangchain4jControllers();
		registerMcpControllers();
	}

	private void registerLangchain4jControllers() {
		// datasource 连接测试控制器由 chat-model 类型配置承载，一个实例服务七类资源；
		// embedding-model 独立装配，缺省时无法测试 embedding-store，传 null 由控制器兜底。
		Langchain4jModelFactoryRegistry factories = applicationContext.getBeanProvider(Langchain4jModelFactoryRegistry.class)
				.getIfAvailable();
		if (factories != null) {
			MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels = applicationContext
					.getBeanProvider(MagicDynamicLangchain4jEmbeddingModelDataSource.class).getIfAvailable();
			mapping.registerController(new Langchain4jDatasourceController(configuration, factories, embeddingModels));
		}
	}

	private void registerMcpControllers() {
		if (applicationContext.getBeanProvider(MagicApiMcpProperties.class).getIfAvailable() == null) {
			return;
		}
		mapping.registerController(new MagicMcpServerController(configuration))
				.registerController(new MagicMcpDiagnosticController(configuration));
	}
}
