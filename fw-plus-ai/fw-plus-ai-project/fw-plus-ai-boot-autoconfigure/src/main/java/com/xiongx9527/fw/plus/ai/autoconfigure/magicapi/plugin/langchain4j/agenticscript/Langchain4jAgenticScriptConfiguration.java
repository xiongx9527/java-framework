package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticPipelineRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module.Langchain4jAgenticScriptModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.registry.Langchain4jAgenticScriptMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.resource.Langchain4jAgenticScriptMagicResourceStorage;
import dev.langchain4j.agentic.UntypedAgent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * {@code langchain4j-agentic-script} 资源插件的装配：路径型 .ms 脚本资源（resource 形态，
 * 与 system-message 同模式——资源即脚本、控制台原生脚本编辑器维护、热更新）、动态注册器、
 * 编排注册中心与脚本模块 {@code langchain4jAgenticScript.node(path)}。
 */
@ConditionalOnClass(name = "dev.langchain4j.agentic.UntypedAgent")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.agentic-script", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jAgenticScriptConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-agentic-script.1.0.0.iife.js";

	public Langchain4jAgenticScriptConfiguration() {
		super("langchain4j-agentic-script", "MagicLangchain4jAgenticScript", JAVASCRIPT_FILENAME);
	}

	/** 执行测试 Controller：POST /langchain4j/agentic-script/execute 返回脚本节点输出。 */
	@Override
	public org.ssssssss.magicapi.core.web.MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> mapping.registerController(
				new com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.web.Langchain4jAgenticScriptController(configuration));
	}

	/** 编排注册中心：key → 编排句柄（UntypedAgent），nl2sql 等提供方注册，资源链与脚本按 key 引用。 */
	@Bean
	@ConditionalOnMissingBean
	public AgenticPipelineRegistry agenticPipelineRegistry() {
		return new AgenticPipelineRegistry();
	}

	/** langchain4j-agentic-script 资源类型存储（路径型 .ms）。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jAgenticScriptMagicResourceStorage langchain4jAgenticScriptMagicResourceStorage() {
		return new Langchain4jAgenticScriptMagicResourceStorage();
	}

	/** langchain4j-agentic-script 动态注册器：资源变更即刷新映射。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jAgenticScriptMagicDynamicRegistry langchain4jAgenticScriptMagicDynamicRegistry(
			Langchain4jAgenticScriptMagicResourceStorage storage) {
		return new Langchain4jAgenticScriptMagicDynamicRegistry(storage);
	}

	/** 脚本模块：langchain4jAgenticScript.node(path) 取脚本节点，toolProvider(path,...) 包装为工具。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jAgenticScriptModule langchain4jAgenticScriptModule(
			Langchain4jAgenticScriptMagicDynamicRegistry registry, MagicResourceService magicResourceService) {
		return new Langchain4jAgenticScriptModule(registry, magicResourceService);
	}
}
