package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.FilteringContentRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.util.Collections;

/**
 * 过滤装饰器：{@code FilteringContentRetriever}，包在另一个 content-retriever 之外按 score 与 metadata 过滤。
 *
 * <p>options：{@code content-retriever-key}（必填，被委托的 retriever）、{@code min-score}、
 * {@code filters}（metadata 键值表，{@code tags}/{@code keywords} 按包含匹配，其余按相等匹配）。</p>
 */
public class FilteringContentRetrieverProvider implements ContentRetrieverProvider {

	@Override
	public String provider() {
		return "filtering";
	}

	@Override
	public ContentRetriever build(Langchain4jOptionsReader o, ContentRetrieverContext context) {
		return new FilteringContentRetriever(context.contentRetriever(o.required("content-retriever-key")),
				o.dbl("min-score", 0.0), o.stringMap("filters", Collections.<String, String>emptyMap()));
	}
}
