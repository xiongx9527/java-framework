package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.retriever.ContentRetriever;

/**
 * 一种 ContentRetriever 实现的构建工厂：每个短名对应一个实现
 * （embedding-store / neo4j / azure-ai-search / web-search / filtering）。
 *
 * <p>参数一律经 {@link Langchain4jOptionsReader} 从 datasource options 读取；
 * 被引用的其它 datasource（向量库、模型、另一个 retriever）经 {@link ContentRetrieverContext} 取用。</p>
 */
public interface ContentRetrieverProvider {

	/** provider 短名，如 "embedding-store" */
	String provider();

	/** 按 options 构建本 provider 的 ContentRetriever */
	ContentRetriever build(Langchain4jOptionsReader options, ContentRetrieverContext context);
}
