package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.Langchain4jToolProviderInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.MagicDynamicLangchain4jToolProviderDataSource;
import dev.langchain4j.service.tool.ToolProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.List;
import java.util.TreeSet;

/**
 * ToolProvider 脚本模块：脚本用 {@code langchain4jToolProvider['key']} 取数据源工作台维护的
 * {@link ToolProvider}，再注入 {@code AiServices.builder(...).toolProvider(...)}。
 */
@MagicModule("langchain4jToolProvider")
public class Langchain4jToolProviderModule
		implements DynamicAttribute<ToolProvider, Langchain4jToolProviderModule>, DynamicModule<ToolProvider> {

	private final MagicDynamicLangchain4jToolProviderDataSource dataSource;

	/** Spring 容器中的默认 ToolProvider（langchain4j 自动配置注入），可能不存在 */
	private final org.springframework.beans.factory.ObjectProvider<ToolProvider> defaultInstance;

	public Langchain4jToolProviderModule(MagicDynamicLangchain4jToolProviderDataSource dataSource,
			org.springframework.beans.factory.ObjectProvider<ToolProvider> defaultInstance) {
		this.dataSource = dataSource;
		this.defaultInstance = defaultInstance;
	}

	@Override
	public ToolProvider getDynamicAttribute(String key) {
		if (key == null || key.trim().isEmpty()) {
			return requireDefaultInstance();
		}
		ToolProvider provider = dataSource.get(key);
		if (provider == null) {
			throw new IllegalArgumentException(String.format(
					"未配置 LangChain4j ToolProvider: %s，已配置: %s", key, new TreeSet<>(dataSource.keys())));
		}
		return provider;
	}

	/** Spring 默认 ToolProvider（可能不存在） */
	private ToolProvider requireDefaultInstance() {
		ToolProvider instance = defaultInstance == null ? null : defaultInstance.getIfAvailable();
		if (instance == null) {
			throw new IllegalArgumentException(
					"未指定 LangChain4j ToolProvider 的 datasource key，且 Spring 容器中没有默认实例"
							+ "（请启用 langchain4j tool-provider 自动配置，或在控制台维护后用 ['key'] 取用）");
		}
		return instance;
	}

	/** 默认 ToolProvider（key=default，只读展示）与控制台配置的数据源，一并暴露 */
	public List<Langchain4jToolProviderInfo> defaultMappings() {
		List<Langchain4jToolProviderInfo> result = new java.util.ArrayList<>();
		if (defaultInstance != null && defaultInstance.getIfAvailable() != null) {
			Langchain4jToolProviderInfo info = new Langchain4jToolProviderInfo();
			info.setKey("default");
			info.setName("默认数据源");
			info.setEnabled(true);
			result.add(info);
		}
		return result;
	}

	@Override
	public Langchain4jToolProviderModule setDynamicAttribute(String key, ToolProvider value) {
		throw new UnsupportedOperationException(
				"脚本不支持设置 ToolProvider，请在控制台维护 langchain4j-tool-provider 数据源");
	}

	@Override
	public ToolProvider getDynamicModule(MagicScriptContext context) {
		String key = context == null ? null : context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		return getDynamicAttribute(key);
	}
}
