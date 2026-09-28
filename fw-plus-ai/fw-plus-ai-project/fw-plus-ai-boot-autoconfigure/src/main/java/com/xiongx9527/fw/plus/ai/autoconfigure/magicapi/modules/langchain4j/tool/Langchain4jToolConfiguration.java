package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool;

import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * Magic-API → LangChain4j 本地工具装配（tool 包入口）。
 *
 * <p>把 magic-api 的 api + function 资源作为 LangChain4j {@link ToolProvider} 暴露（本地调用，
 * 不经 MCP）。使用方把 {@link MagicApiToolProvider} bean 注入
 * {@code AiServices.builder(Svc.class).toolProvider(...)} 即可。</p>
 *
 * <p>生效条件：langchain4j 在 classpath、{@link MagicResourceService} bean 已注册
 * （即 magic-api starter 启用）、{@code fw.plus.magicapi.langchain4j.tool.enabled} 未关闭。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ToolProvider.class)
@ConditionalOnBean(MagicResourceService.class)
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.tool", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(Langchain4jToolProperties.class)
public class Langchain4jToolConfiguration {

	@Bean
	public MagicApiToolRegistry magicApiToolRegistry(MagicResourceService magicResourceService,
			Langchain4jToolProperties properties) {
		return new MagicApiToolRegistry(magicResourceService, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public MagicToolInvoker magicToolInvoker() {
		return new ScriptManagerMagicToolInvoker();
	}

	@Bean
	public MagicApiToolExecutor magicApiToolExecutor(MagicApiToolRegistry registry,
			ObjectProvider<MagicToolInvoker> invokerProvider) {
		MagicToolInvoker invoker = invokerProvider.getIfAvailable(ScriptManagerMagicToolInvoker::new);
		return new MagicApiToolExecutor(registry, invoker);
	}

	@Bean
	public MagicApiToolProvider magicApiToolProvider(MagicApiToolRegistry registry, MagicApiToolExecutor executor) {
		return new MagicApiToolProvider(registry, executor);
	}

	@Bean
	public MagicApiToolChangeListener magicApiToolChangeListener(MagicApiToolRegistry registry) {
		return new MagicApiToolChangeListener(registry);
	}
}
