package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;

/**
 * 联网检索：{@code WebSearchContentRetriever}，搜索引擎由 {@code search-function} 托管，
 * 未配置时回落 Spring 容器中的 {@code WebSearchEngine} 实例。
 *
 * <p>options：{@code search-function}、{@code max-results}。</p>
 */
public class WebSearchContentRetrieverProvider implements ContentRetrieverProvider {

	@Override
	public String provider() {
		return "web-search";
	}

	@Override
	public ContentRetriever build(Langchain4jOptionsReader o, ContentRetrieverContext context) {
		WebSearchContentRetriever.WebSearchContentRetrieverBuilder builder = WebSearchContentRetriever.builder()
				.webSearchEngine(context.webSearchEngine(o.string("search-function", null)));
		if (o.has("max-results")) {
			builder.maxResults(o.integer("max-results", null));
		}
		return builder.build();
	}
}
