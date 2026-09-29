package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.module.Langchain4jSystemMessageProviderModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.registry.Langchain4jSystemMessageMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.resource.Langchain4jSystemMessageMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.web.Langchain4jSystemMessageController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * {@code langchain4j-system-message} 资源插件的装配：资源存储、动态注册器与脚本模块
 * {@code langchain4jSystemMessageProvider}。
 *
 * <p>把 magic-api 的资源（{@code .ms} 脚本）作为 LangChain4j 系统提示词暴露为
 * {@code Function<Object, String>}，供 {@code AiServices.systemMessageProvider(...)} 装配。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.service.AiServices")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.system-message", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jSystemMessageProviderConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-system-message.1.0.0.iife.js";

	public Langchain4jSystemMessageProviderConfiguration() {
		super("langchain4j-system-message", "MagicLangchain4jSystemMessage", JAVASCRIPT_FILENAME);
	}

	/** 执行测试 Controller：POST /langchain4j/system-message/execute 返回系统提示词文本。 */
	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> mapping.registerController(new Langchain4jSystemMessageController(configuration));
	}

	/** langchain4j-system-message 资源类型存储。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jSystemMessageMagicResourceStorage langchain4jSystemMessageMagicResourceStorage() {
		return new Langchain4jSystemMessageMagicResourceStorage();
	}

	/** langchain4j-system-message 动态注册表：资源变更即增删系统提示词映射。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jSystemMessageMagicDynamicRegistry langchain4jSystemMessageMagicDynamicRegistry(
			Langchain4jSystemMessageMagicResourceStorage storage) {
		return new Langchain4jSystemMessageMagicDynamicRegistry(storage);
	}

	/** 脚本模块：langchain4jSystemMessageProvider.get(path) 返回 Function<Object, String>。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jSystemMessageProviderModule langchain4jSystemMessageProviderModule(
			Langchain4jSystemMessageMagicDynamicRegistry registry, MagicResourceService magicResourceService) {
		return new Langchain4jSystemMessageProviderModule(registry, magicResourceService);
	}
}
