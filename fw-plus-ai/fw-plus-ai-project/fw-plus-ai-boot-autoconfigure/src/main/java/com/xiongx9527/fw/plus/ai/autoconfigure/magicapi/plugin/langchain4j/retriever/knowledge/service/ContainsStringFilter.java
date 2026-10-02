package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.embedding.filter.Filter;

/**
 * 自定义 contains 过滤器：langchain4j 0.35.0 的 MetadataFilterBuilder 无 containsString，
 * 1.x 起原生提供 {@code ContainsString}（见 docs/springboot3-upgrade-notes.md，升级后替换为原生下推）。
 *
 * <p>各向量库的 filter 翻译器不识别本类型，下推会失败并自动回落内存过滤（语义正确，性能次优）。</p>
 */
class ContainsStringFilter implements Filter {

	private final String key;

	private final String substring;

	ContainsStringFilter(String key, String substring) {
		this.key = key;
		this.substring = substring;
	}

	static ContainsStringFilter of(String key, String substring) {
		return new ContainsStringFilter(key, substring == null ? "" : substring);
	}

	@Override
	public boolean test(Object object) {
		if (object instanceof Metadata) {
			String value = ((Metadata) object).getString(key);
			return value != null && value.contains(substring);
		}
		return testMap(metadataMapOf(object));
	}

	/** 供内存过滤对纯 Map 评估（文档 metadata 的最新值为准）。 */
	boolean testMap(java.util.Map<String, Object> map) {
		Object value = map == null ? null : map.get(key);
		return value != null && String.valueOf(value).contains(substring);
	}

	@SuppressWarnings("unchecked")
	private static java.util.Map<String, Object> metadataMapOf(Object object) {
		if (object instanceof java.util.Map) {
			return (java.util.Map<String, Object>) object;
		}
		return null;
	}
}
