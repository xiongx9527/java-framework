package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticPipelineRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.MagicDynamicLangchain4jAgenticScriptDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module.Langchain4jAgenticScriptModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.registry.Langchain4jAgenticScriptMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.resource.Langchain4jAgenticScriptMagicResourceStorage;
import dev.langchain4j.agentic.UntypedAgent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code langchain4j-agentic-script} 资源的装配：脚本节点实例持有者、资源存储、动态注册器
 * 与脚本模块 {@code langchain4jAgenticScript['key']}，并提供编排注册中心
 * {@link AgenticPipelineRegistry}（nl2sql 等编排提供方按 key 注册，tool-provider 资源与
 * 脚本按 key 引用）。
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

	/** 编排注册中心：key → 编排句柄（UntypedAgent）。 */
	@Bean
	@ConditionalOnMissingBean
	public AgenticPipelineRegistry agenticPipelineRegistry() {
		return new AgenticPipelineRegistry();
	}

	/** 脚本节点实例持有者：按 key 持有多个脚本节点。 */
	@Bean
	@ConditionalOnMissingBean
	public MagicDynamicLangchain4jAgenticScriptDataSource langchain4jAgenticScriptDataSource() {
		return new MagicDynamicLangchain4jAgenticScriptDataSource();
	}

	/** langchain4j-agentic-script 资源类型存储。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jAgenticScriptMagicResourceStorage langchain4jAgenticScriptMagicResourceStorage() {
		return new Langchain4jAgenticScriptMagicResourceStorage();
	}

	/** langchain4j-agentic-script 动态注册器：资源变更即重建/注销脚本节点。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jAgenticScriptMagicDynamicRegistry langchain4jAgenticScriptMagicDynamicRegistry(
			Langchain4jAgenticScriptMagicResourceStorage storage,
			MagicDynamicLangchain4jAgenticScriptDataSource dataSource) {
		return new Langchain4jAgenticScriptMagicDynamicRegistry(storage, dataSource);
	}

	/** 脚本模块：langchain4jAgenticScript['key'] 取脚本节点，toolProvider(...) 包装为工具。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jAgenticScriptModule langchain4jAgenticScriptModule(
			MagicDynamicLangchain4jAgenticScriptDataSource dataSource) {
		return new Langchain4jAgenticScriptModule(dataSource);
	}
}
