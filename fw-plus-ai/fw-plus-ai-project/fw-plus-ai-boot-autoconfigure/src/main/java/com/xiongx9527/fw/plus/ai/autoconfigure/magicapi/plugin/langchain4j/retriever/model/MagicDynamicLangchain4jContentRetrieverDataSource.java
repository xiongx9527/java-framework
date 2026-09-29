package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code langchain4j-content-retriever} 数据源的实例持有者：按 {@code key} 持有与驱逐
 * 多个 {@link ContentRetriever} 实例，驱逐时关闭实现 {@link Closeable} 的实例。
 */
public class MagicDynamicLangchain4jContentRetrieverDataSource implements DisposableBean {

	private static final Logger logger =
			LoggerFactory.getLogger(MagicDynamicLangchain4jContentRetrieverDataSource.class);

	private final Map<String, ContentRetriever> retrievers = new ConcurrentHashMap<>();

	/** 存入实例；同 key 覆盖时不关闭旧实例，调用方应先 {@link #evict(String)}。 */
	public void put(String key, ContentRetriever retriever) {
		if (key == null) {
			return;
		}
		retrievers.put(key, retriever);
	}

	/** 按 key 取实例，不存在时返回 {@code null}。 */
	public ContentRetriever get(String key) {
		return key == null ? null : retrievers.get(key);
	}

	/** 驱逐实例：可关闭的实例会被关闭，关闭异常只记日志。 */
	public void evict(String key) {
		if (key == null) {
			return;
		}
		close(retrievers.remove(key), key);
	}

	/** 当前已配置的 key 集合（不可修改视图）。 */
	public Set<String> keys() {
		return Collections.unmodifiableSet(retrievers.keySet());
	}

	@Override
	public void destroy() {
		for (String key : new ArrayList<>(retrievers.keySet())) {
			evict(key);
		}
	}

	private void close(ContentRetriever retriever, String key) {
		if (!(retriever instanceof Closeable)) {
			return;
		}
		try {
			((Closeable) retriever).close();
		} catch (Exception e) {
			logger.warn("关闭 LangChain4j ContentRetriever 失败: key={}", key, e);
		}
	}
}
