package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.MagicDynamicLangchain4jToolProviderDataSource;
import dev.langchain4j.service.tool.ToolProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.TreeSet;

/**
 * ToolProvider 脚本模块：脚本用 {@code langchain4jToolProvider['key']} 取数据源工作台维护的
 * {@link ToolProvider}，再注入 {@code AiServices.builder(...).toolProvider(...)}。
 */
@MagicModule("langchain4jToolProvider")
public class Langchain4jToolProviderModule
		implements DynamicAttribute<ToolProvider, Langchain4jToolProviderModule>, DynamicModule<ToolProvider> {

	private final MagicDynamicLangchain4jToolProviderDataSource dataSource;

	public Langchain4jToolProviderModule(MagicDynamicLangchain4jToolProviderDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public ToolProvider getDynamicAttribute(String key) {
		if (key == null || key.trim().isEmpty()) {
			throw new IllegalArgumentException("请通过 langchain4jToolProvider['key'] 指定数据源 key");
		}
		ToolProvider provider = dataSource.get(key);
		if (provider == null) {
			throw new IllegalArgumentException(String.format(
					"未配置 LangChain4j ToolProvider: %s，已配置: %s", key, new TreeSet<>(dataSource.keys())));
		}
		return provider;
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
