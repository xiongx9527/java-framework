package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.GsonChatMessageJsonCodec;
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
 * {@link ChatMemoryStore} 实例，每个实例把 get/update/delete 委托给各自配置的函数。
 */
public class MagicDynamicLangchain4jChatMemoryStoreDataSource implements DisposableBean {

	private static final Logger logger =
			LoggerFactory.getLogger(MagicDynamicLangchain4jChatMemoryStoreDataSource.class);

	private final MagicToolInvoker invoker;

	private final ChatMessageJsonCodec codec = new GsonChatMessageJsonCodec();

	private final Map<String, ChatMemoryStore> stores = new ConcurrentHashMap<>();

	public MagicDynamicLangchain4jChatMemoryStoreDataSource(MagicToolInvoker invoker) {
		this.invoker = invoker;
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
			MagicApiChatMemoryStore store = new MagicApiChatMemoryStore(invoker, codec,
					info.getGetFunction(), info.getUpdateFunction(), info.getDeleteFunction());
			stores.put(key, store);
			logger.info("注册 LangChain4j ChatMemoryStore: {}", key);
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
