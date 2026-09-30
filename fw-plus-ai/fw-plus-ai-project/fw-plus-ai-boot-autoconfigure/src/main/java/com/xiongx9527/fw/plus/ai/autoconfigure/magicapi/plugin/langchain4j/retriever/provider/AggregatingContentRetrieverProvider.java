package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.aggregate.AggregatingContentRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合检索：{@code AggregatingContentRetriever}，把多个检索器的结果合并成一份。
 *
 * <p>options：{@code content-retriever-keys}（必填，被组合的 retriever key，多个用逗号分隔）、
 * {@code max-results}（合并条数上限，0/缺省表示不限）、{@code deduplicate}（按正文去重，默认 true）。</p>
 */
public class AggregatingContentRetrieverProvider implements ContentRetrieverProvider {

	@Override
	public String provider() {
		return "aggregating";
	}

	@Override
	public ContentRetriever build(Langchain4jOptionsReader o, ContentRetrieverContext context) {
		o.required("content-retriever-keys");
		List<String> keys = o.stringList("content-retriever-keys", null);
		List<ContentRetriever> delegates = new ArrayList<>();
		for (String key : keys) {
			delegates.add(context.contentRetriever(key));
		}
		return new AggregatingContentRetriever(delegates, keys,
				o.integer("max-results", 0), o.bool("deduplicate", true));
	}
}
