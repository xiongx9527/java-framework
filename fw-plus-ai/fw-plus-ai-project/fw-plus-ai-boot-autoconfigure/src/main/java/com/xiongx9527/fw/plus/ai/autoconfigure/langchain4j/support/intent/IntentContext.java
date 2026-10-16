package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 意图识别上下文：待判定的文本 + 可选的附加属性。
 *
 * <p>不同接入点用不同文本构造：RAG 取 {@code Query.text()}，工具提供者取
 * {@code UserMessage.singleText()}，编排步骤取当前请求文本。附加属性（如租户、渠道）
 * 供自定义分类器使用，本组件的内置层级不依赖它。</p>
 */
public final class IntentContext {

	private final String text;

	private final Map<String, String> attributes;

	private IntentContext(String text, Map<String, String> attributes) {
		this.text = text;
		this.attributes = attributes == null || attributes.isEmpty()
				? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, String>(attributes));
	}

	public static IntentContext of(String text) {
		return new IntentContext(text, null);
	}

	public static IntentContext of(String text, Map<String, String> attributes) {
		return new IntentContext(text, attributes);
	}

	/** 原始文本，可能为 null。 */
	public String text() {
		return text;
	}

	/** 去空白后的文本，空文本返回空串（不返回 null）。 */
	public String normalizedText() {
		return text == null ? "" : text.trim();
	}

	public Map<String, String> attributes() {
		return attributes;
	}

	public String attribute(String name) {
		return name == null ? null : attributes.get(name);
	}

	@Override
	public String toString() {
		return "IntentContext{text='" + text + "', attributes=" + attributes + '}';
	}
}
