package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.MagicDynamicLangchain4jToolProviderDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.module.Langchain4jToolProviderModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.registry.Langchain4jToolProviderMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.resource.Langchain4jToolProviderMagicResourceStorage;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * {@code langchain4j-tool-provider} 数据源的装配：实例持有者、资源存储、动态注册器与脚本模块
 * {@code langchain4jToolProvider['key']}。
 *
 * <p>把 magic-api 的 api + function 资源作为 LangChain4j {@link ToolProvider} 本地暴露（不经 MCP），
 * 每个数据源实例按各自的工具范围配置独立构建。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.service.tool.ToolProvider")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.tool-provider", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jToolProviderConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-tool-provider.1.0.0.iife.js";

	public Langchain4jToolProviderConfiguration() {
		super("langchain4j-tool-provider", "MagicLangchain4jToolProvider", JAVASCRIPT_FILENAME);
	}

	/** 脚本执行器：function 走 magic.invoke，api 走脚本执行；可用自定义实现覆盖。 */
	@Bean
	@ConditionalOnMissingBean
	public MagicToolInvoker magicToolInvoker() {
		return new ScriptManagerMagicToolInvoker();
	}

	/** 数据源实例持有者：按 key 持有多个 ToolProvider 实例。 */
	@Bean
	@ConditionalOnMissingBean
	public MagicDynamicLangchain4jToolProviderDataSource langchain4jToolProviderDataSource(
			MagicResourceService magicResourceService, MagicToolInvoker invoker) {
		return new MagicDynamicLangchain4jToolProviderDataSource(magicResourceService, invoker);
	}

	/** langchain4j-tool-provider 资源类型存储。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jToolProviderMagicResourceStorage langchain4jToolProviderMagicResourceStorage() {
		return new Langchain4jToolProviderMagicResourceStorage();
	}

	/** langchain4j-tool-provider 动态注册器：资源变更即增删 ToolProvider 实例。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jToolProviderMagicDynamicRegistry langchain4jToolProviderMagicDynamicRegistry(
			Langchain4jToolProviderMagicResourceStorage storage,
			MagicDynamicLangchain4jToolProviderDataSource dataSource,
			ObjectProvider<ToolProvider> defaultInstance) {
		return new Langchain4jToolProviderMagicDynamicRegistry(storage, dataSource, defaultInstance);
	}

	/** 脚本模块：langchain4jToolProvider['key'] 按 key 取 ToolProvider。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jToolProviderModule langchain4jToolProviderModule(
			MagicDynamicLangchain4jToolProviderDataSource dataSource,
			org.springframework.beans.factory.ObjectProvider<ToolProvider> defaultInstance) {
		return new Langchain4jToolProviderModule(dataSource, defaultInstance);
	}

	/** api/function 资源变更后失效全部实例的工具快照。 */
	@Bean
	public MagicApiToolChangeListener magicApiToolChangeListener(
			MagicDynamicLangchain4jToolProviderDataSource dataSource) {
		return new MagicApiToolChangeListener(dataSource);
	}
}
