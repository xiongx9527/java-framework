package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Magic-API 函数托管的检索结果过滤：把「保留哪些条目」交给一个 magic-api 函数决定，
 * 用于内置的 score 阈值 / metadata 过滤表达不了的场景（改写正文、按业务规则合并、调外部接口判断等）。
 *
 * <p>函数入参：</p>
 * <ul>
 *   <li>{@code contents}：检索结果 {@code List<Content>}（原对象）；</li>
 *   <li>{@code texts}：上面各条正文的 {@code List<String>} 便捷视图；</li>
 *   <li>{@code query}：本次检索的查询文本。</li>
 * </ul>
 *
 * <p>函数返回值：{@code List<Content>} 原样使用；{@code List<String>} 或 JSON 数组字符串按「每条一项」转成
 * {@link Content}；返回 {@code null} 视为不做过滤（保留全部）。</p>
 */
public class MagicApiContentFilter {

	private static final String CONTENTS = "contents";

	private static final String TEXTS = "texts";

	private static final String QUERY = "query";

	private final MagicToolInvoker invoker;

	private final String filterFunction;

	public MagicApiContentFilter(MagicToolInvoker invoker, String filterFunction) {
		this.invoker = invoker;
		this.filterFunction = filterFunction;
	}

	/** 执行过滤函数；函数未返回内容时保留入参（不做过滤） */
	public List<Content> filter(List<Content> contents, Query query) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put(CONTENTS, contents);
		arguments.put(TEXTS, textsOf(contents));
		arguments.put(QUERY, query == null ? null : query.text());
		Object result = invoker.invokeFunction(filterFunction, arguments);
		if (result == null) {
			return contents;
		}
		return toContents(result);
	}

	/** 函数返回值归一成 List<Content>：Content 原样，其余按文本包成 Content；支持 JSON 数组字符串 */
	static List<Content> toContents(Object result) {
		if (result == null) {
			return Collections.emptyList();
		}
		List<?> items;
		if (result instanceof List) {
			items = (List<?>) result;
		} else if (result instanceof String) {
			String text = ((String) result).trim();
			if (text.isEmpty()) {
				return Collections.emptyList();
			}
			items = JSON.parseArray(text);
		} else {
			throw new IllegalStateException("过滤函数返回值类型不支持: " + result.getClass().getName());
		}
		List<Content> contents = new ArrayList<>();
		for (Object item : items) {
			contents.add(item instanceof Content ? (Content) item : Content.from(String.valueOf(item)));
		}
		return contents;
	}

	private static List<String> textsOf(List<Content> contents) {
		List<String> texts = new ArrayList<>();
		if (contents != null) {
			for (Content content : contents) {
				texts.add(content == null || content.textSegment() == null ? "" : content.textSegment().text());
			}
		}
		return texts;
	}
}
