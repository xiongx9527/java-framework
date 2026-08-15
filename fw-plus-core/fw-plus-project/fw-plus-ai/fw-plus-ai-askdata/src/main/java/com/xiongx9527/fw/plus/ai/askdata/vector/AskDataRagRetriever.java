package com.xiongx9527.fw.plus.ai.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.List;

/**
 * 通过中立向量存储执行 RAG 召回的轻量入口。
 *
 * <p>本类无共享可变状态，可并发复用。权限过滤责任仍由 {@link AskDataVectorStore} 和请求 filters 承担；
 * 本类不会放宽条件、跨操作人缓存或把召回文档直接视为可执行 SQL。</p>
 */
public final class AskDataRagRetriever {

	private final AskDataVectorStore store;

	public AskDataRagRetriever(AskDataVectorStore store) {
		if (store == null) {
			throw new IllegalArgumentException("store 不能为空");
		}
		this.store = store;
	}

	public List<AskDataVectorDocument> retrieve(AskDataProjectContext context,
			AskDataVectorSearchRequest request) {
		return store.search(context, request);
	}
}
