package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code langchain4j-chat-memory-store} 数据源的实例持有者：按 {@code key} 持有并驱逐多个
 * {@link ChatMemoryStore} 实例，实例由 {@link ChatMemoryStoreProviderFactory} 按
 * 资源定义的 {@code type}（提供商）构建。
 */
public class MagicDynamicLangchain4jChatMemoryStoreDataSource implements DisposableBean {

	private static final Logger logger =
			LoggerFactory.getLogger(MagicDynamicLangchain4jChatMemoryStoreDataSource.class);

	private final ChatMemoryStoreProviderFactory providerFactory;

	private final Map<String, ChatMemoryStore> stores = new ConcurrentHashMap<>();

	public MagicDynamicLangchain4jChatMemoryStoreDataSource(ChatMemoryStoreProviderFactory providerFactory) {
		this.providerFactory = providerFactory;
	}

	/**
	 * 按资源定义创建（或重建）一个 ChatMemoryStore 实例。
	 */
	public boolean create(Langchain4jChatMemoryStoreInfo info) {
		String key = info.getKey();
		if (key == null || key.trim().isEmpty()) {
			return false;
		}
		try {
			destroy(key);
			ChatMemoryStore store = providerFactory.create(info);
			stores.put(key, store);
			logger.info("注册 LangChain4j ChatMemoryStore: {} (type={})", key, info.getType());
			return true;
		} catch (Throwable e) {
			logger.warn("构建 LangChain4j ChatMemoryStore 失败: key={}", key, e);
			return false;
		}
	}

	/**
	 * 按 key 销毁实例。
	 */
	public void destroy(String key) {
		if (key == null) {
			return;
		}
		stores.remove(key);
	}

	/**
	 * 按 key 取 ChatMemoryStore，不存在时返回 {@code null}。
	 */
	public ChatMemoryStore get(String key) {
		return key == null ? null : stores.get(key);
	}

	/**
	 * 当前已配置的 key 集合（不可修改视图）。
	 */
	public Set<String> keys() {
		return Collections.unmodifiableSet(stores.keySet());
	}

	@Override
	public void destroy() {
		stores.clear();
	}
}
