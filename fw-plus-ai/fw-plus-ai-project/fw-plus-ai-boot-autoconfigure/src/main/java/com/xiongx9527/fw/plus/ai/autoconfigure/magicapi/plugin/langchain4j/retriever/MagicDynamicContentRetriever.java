package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.MagicDynamicLangchain4jContentRetrieverDataSource;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.List;

/**
 * 按 key 懒解析另一个 {@link ContentRetriever}：每次检索时才取实例，
 * 因此被委托的 retriever 可以先于、也可以晚于本实例注册（filtering 的委托链依赖这点）。
 */
public class MagicDynamicContentRetriever implements ContentRetriever {

	private final MagicDynamicLangchain4jContentRetrieverDataSource retrievers;

	private final String key;

	public MagicDynamicContentRetriever(MagicDynamicLangchain4jContentRetrieverDataSource retrievers, String key) {
		this.retrievers = retrievers;
		this.key = key;
	}

	@Override
	public List<Content> retrieve(Query query) {
		return resolve().retrieve(query);
	}

	private ContentRetriever resolve() {
		ContentRetriever retriever = retrievers.get(key);
		if (retriever != null) {
			return retriever;
		}
		if (StringUtils.isEmpty(key)) {
			return SpringUtils.getBean(ContentRetriever.class);
		}
		return SpringUtils.getBean(key, ContentRetriever.class);
	}
}
