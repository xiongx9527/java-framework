package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.module.Langchain4jContentInjectorModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.registry.Langchain4jContentInjectorMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.resource.Langchain4jContentInjectorMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.web.Langchain4jContentInjectorController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * {@code langchain4j-content-injector} 资源插件的装配：资源存储、动态注册表、脚本模块
 * {@code langchain4jContentInjector} 与执行测试 Controller。
 *
 * <p>把 magic-api 的 {@code .ms} 脚本作为 LangChain4j {@code ContentInjector} 暴露，供
 * {@code DefaultRetrievalAugmentor.builder().contentInjector(...)} 装配——检索结果怎么拼进
 * user message 完全由脚本决定。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.rag.content.injector.ContentInjector")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.content-injector", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jContentInjectorConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-content-injector.1.0.0.iife.js";

	/** 惰性依赖：模块 Bean 由本类声明，构造期尚未就绪，注册 controller 时才取用 */
	private final ObjectProvider<Langchain4jContentInjectorModule> moduleProvider;

	public Langchain4jContentInjectorConfiguration(ObjectProvider<Langchain4jContentInjectorModule> moduleProvider) {
		super("langchain4j-content-injector", "MagicLangchain4jContentInjector", JAVASCRIPT_FILENAME, false);
		this.moduleProvider = moduleProvider;
	}

	/** 执行测试 Controller：POST /langchain4j/content-injector/execute 返回注入结果。 */
	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> mapping.registerController(
				new Langchain4jContentInjectorController(configuration, moduleProvider.getObject()));
	}

	/** langchain4j-content-injector 资源类型存储。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jContentInjectorMagicResourceStorage langchain4jContentInjectorMagicResourceStorage() {
		return new Langchain4jContentInjectorMagicResourceStorage();
	}

	/** langchain4j-content-injector 动态注册表：资源变更即刷新注入器映射。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jContentInjectorMagicDynamicRegistry langchain4jContentInjectorMagicDynamicRegistry(
			Langchain4jContentInjectorMagicResourceStorage storage) {
		return new Langchain4jContentInjectorMagicDynamicRegistry(storage);
	}

	/** 脚本模块：langchain4jContentInjector.get(path) 返回 ContentInjector。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jContentInjectorModule langchain4jContentInjectorModule(
			Langchain4jContentInjectorMagicDynamicRegistry registry, MagicResourceService magicResourceService) {
		return new Langchain4jContentInjectorModule(registry, magicResourceService);
	}
}
