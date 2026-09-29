package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.Langchain4jChatMemoryStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.MagicDynamicLangchain4jChatMemoryStoreDataSource;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * ChatMemoryStore 脚本模块：脚本用 {@code langchain4jChatMemoryStore['key']} 取数据源工作台维护的
 * {@link ChatMemoryStore}，再注入 {@code MessageWindowChatMemory.chatMemoryStore(...)}。
 */
@MagicModule("langchain4jChatMemoryStore")
public class Langchain4jChatMemoryStoreModule implements
		DynamicAttribute<ChatMemoryStore, Langchain4jChatMemoryStoreModule>, DynamicModule<ChatMemoryStore> {

	private final MagicDynamicLangchain4jChatMemoryStoreDataSource dataSource;

	/** Spring 容器中的默认 ChatMemoryStore（langchain4j 自动配置注入），可能不存在 */
	private final org.springframework.beans.factory.ObjectProvider<ChatMemoryStore> defaultInstance;

	public Langchain4jChatMemoryStoreModule(MagicDynamicLangchain4jChatMemoryStoreDataSource dataSource,
			org.springframework.beans.factory.ObjectProvider<ChatMemoryStore> defaultInstance) {
		this.dataSource = dataSource;
		this.defaultInstance = defaultInstance;
	}

	@Override
	public ChatMemoryStore getDynamicAttribute(String key) {
		if (key == null || key.trim().isEmpty()) {
			return requireDefaultInstance();
		}
		ChatMemoryStore store = dataSource.get(key);
		if (store == null) {
			throw new IllegalArgumentException(String.format(
					"未配置 LangChain4j ChatMemoryStore: %s，已配置: %s", key, new TreeSet<>(dataSource.keys())));
		}
		return store;
	}

	/** Spring 默认 ChatMemoryStore（可能不存在） */
	private ChatMemoryStore requireDefaultInstance() {
		ChatMemoryStore instance = defaultInstance == null ? null : defaultInstance.getIfAvailable();
		if (instance == null) {
			throw new IllegalArgumentException(
					"未指定 LangChain4j ChatMemoryStore 的 datasource key，且 Spring 容器中没有默认实例"
							+ "（请启用 langchain4j chat-memory 自动配置，或在控制台维护后用 ['key'] 取用）");
		}
		return instance;
	}

	/** 默认 ChatMemoryStore（key=default，只读展示）与控制台配置的数据源，一并暴露 */
	public List<Langchain4jChatMemoryStoreInfo> defaultMappings() {
		List<Langchain4jChatMemoryStoreInfo> result = new ArrayList<>();
		if (defaultInstance != null && defaultInstance.getIfAvailable() != null) {
			Langchain4jChatMemoryStoreInfo info = new Langchain4jChatMemoryStoreInfo();
			info.setKey("default");
			info.setName("默认数据源");
			info.setEnabled(true);
			result.add(info);
		}
		return result;
	}

	@Override
	public Langchain4jChatMemoryStoreModule setDynamicAttribute(String key, ChatMemoryStore value) {
		throw new UnsupportedOperationException(
				"脚本不支持设置 ChatMemoryStore，请在控制台维护 langchain4j-chat-memory-store 数据源");
	}

	@Override
	public ChatMemoryStore getDynamicModule(MagicScriptContext context) {
		String key = context == null ? null : context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		return getDynamicAttribute(key);
	}
}
