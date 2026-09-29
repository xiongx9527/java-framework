package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Magic-API 函数托管的 {@link WebSearchEngine}：把搜索请求交给一个 magic-api 函数执行，
 * 插件只做「函数返回值 ↔ {@link WebSearchResults}」的转换。
 *
 * <p>langchain4j 的搜索引擎实现都是独立 SDK 依赖（google-custom / tavily …），
 * 走函数托管可以先用脚本接任意搜索服务（内部接口、数据库、第三方 HTTP 均可）。</p>
 *
 * <p>函数契约（参数见 {@link #arguments(WebSearchRequest)}）：返回对象数组，每个元素形如
 * {@code {title, url, snippet, content, metadata}}，也可直接返回同样结构的 JSON 数组字符串。</p>
 */
public class MagicApiWebSearchEngine implements WebSearchEngine {

	private static final String TITLE = "title";

	private static final String URL = "url";

	private static final String SNIPPET = "snippet";

	private static final String CONTENT = "content";

	private static final String METADATA = "metadata";

	private final MagicToolInvoker invoker;

	private final String searchFunction;

	public MagicApiWebSearchEngine(MagicToolInvoker invoker, String searchFunction) {
		this.invoker = invoker;
		this.searchFunction = searchFunction;
	}

	@Override
	public WebSearchResults search(String query) {
		return search(WebSearchRequest.from(query));
	}

	@Override
	public WebSearchResults search(WebSearchRequest request) {
		Object result = invoker.invokeFunction(searchFunction, arguments(request));
		List<WebSearchOrganicResult> results = new ArrayList<>();
		for (Object item : toList(result)) {
			results.add(toResult(item));
		}
		return WebSearchResults.from(null, results);
	}

	/** 透传 WebSearchRequest 的全部字段，脚本自行决定用哪些。 */
	private static Map<String, Object> arguments(WebSearchRequest request) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("searchTerms", request.searchTerms());
		arguments.put("maxResults", request.maxResults());
		arguments.put("language", request.language());
		arguments.put("geoLocation", request.geoLocation());
		arguments.put("startPage", request.startPage());
		arguments.put("startIndex", request.startIndex());
		arguments.put("safeSearch", request.safeSearch());
		return arguments;
	}

	/** 函数返回值归一成 List：支持 List / JSON 数组字符串 / 空。 */
	private static List<?> toList(Object result) {
		if (result == null) {
			return Collections.emptyList();
		}
		if (result instanceof List) {
			return (List<?>) result;
		}
		if (result instanceof String) {
			String text = ((String) result).trim();
			if (text.isEmpty()) {
				return Collections.emptyList();
			}
			return JSON.parseArray(text);
		}
		throw new IllegalStateException("searchFunction 返回值类型不支持: " + result.getClass().getName());
	}

	@SuppressWarnings("unchecked")
	private static WebSearchOrganicResult toResult(Object item) {
		if (!(item instanceof Map)) {
			throw new IllegalStateException("searchFunction 返回的条目不是对象: " + item);
		}
		Map<String, Object> entry = (Map<String, Object>) item;
		String url = text(entry.get(URL));
		return WebSearchOrganicResult.from(text(entry.get(TITLE)),
				url == null ? null : URI.create(url),
				text(entry.get(SNIPPET)),
				text(entry.get(CONTENT)),
				toMetadata(entry.get(METADATA)));
	}

	private static String text(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> toMetadata(Object value) {
		if (!(value instanceof Map)) {
			return null;
		}
		Map<String, String> metadata = new HashMap<>();
		for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
			metadata.put(entry.getKey(), text(entry.getValue()));
		}
		return metadata;
	}
}
