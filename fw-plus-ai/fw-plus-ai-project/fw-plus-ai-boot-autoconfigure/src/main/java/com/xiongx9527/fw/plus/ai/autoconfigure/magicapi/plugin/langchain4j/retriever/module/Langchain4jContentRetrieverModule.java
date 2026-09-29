package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.MagicDynamicLangchain4jContentRetrieverDataSource;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * ContentRetriever 脚本模块：脚本用 {@code langchain4jContentRetriever['key']} 取
 * 数据源工作台维护的 {@link ContentRetriever}，再注入 {@code RetrievalAugmentor} 等 RAG 组件。
 */
@MagicModule("langchain4jContentRetriever")
public class Langchain4jContentRetrieverModule
		implements DynamicAttribute<ContentRetriever, Langchain4jContentRetrieverModule>,
		DynamicModule<ContentRetriever> {

	private final MagicDynamicLangchain4jContentRetrieverDataSource dataSource;

	/** Spring 容器中的默认 ContentRetriever（core 层 langchain4j 自动配置注入），可能不存在 */
	private final ObjectProvider<ContentRetriever> defaultInstance;

	public Langchain4jContentRetrieverModule(MagicDynamicLangchain4jContentRetrieverDataSource dataSource,
			ObjectProvider<ContentRetriever> defaultInstance) {
		this.dataSource = dataSource;
		this.defaultInstance = defaultInstance;
	}

	@Override
	public ContentRetriever getDynamicAttribute(String key) {
		if (key == null || key.trim().isEmpty()) {
			return requireDefaultInstance();
		}
		ContentRetriever retriever = dataSource.get(key);
		if (retriever == null) {
			throw new IllegalArgumentException(String.format(
					"未配置 LangChain4j ContentRetriever: %s，已配置: %s", key, new TreeSet<>(dataSource.keys())));
		}
		return retriever;
	}

	/** 默认条目（Spring 注入实例，只读展示）+ 控制台配置的数据源，一并暴露 */
	public List<Langchain4jContentRetrieverInfo> defaultMappings() {
		List<Langchain4jContentRetrieverInfo> result = new ArrayList<>();
		if (defaultInstance != null && defaultInstance.getIfAvailable() != null) {
			Langchain4jContentRetrieverInfo info = new Langchain4jContentRetrieverInfo();
			info.setKey("default");
			info.setName("默认数据源");
			info.setEnabled(true);
			result.add(info);
		}
		return result;
	}

	@Override
	public Langchain4jContentRetrieverModule setDynamicAttribute(String key, ContentRetriever value) {
		throw new UnsupportedOperationException(
				"脚本不支持设置 ContentRetriever，请在控制台维护 langchain4j-content-retriever 数据源");
	}

	@Override
	public ContentRetriever getDynamicModule(MagicScriptContext context) {
		String key = context == null ? null : context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		return getDynamicAttribute(key);
	}

	private ContentRetriever requireDefaultInstance() {
		ContentRetriever instance = defaultInstance == null ? null : defaultInstance.getIfAvailable();
		if (instance == null) {
			throw new IllegalArgumentException("未指定 LangChain4j ContentRetriever 的 datasource key，"
					+ "且 Spring 容器中没有默认实例（请在控制台维护 langchain4j-content-retriever 数据源后用 ['key'] 取用）");
		}
		return instance;
	}
}
