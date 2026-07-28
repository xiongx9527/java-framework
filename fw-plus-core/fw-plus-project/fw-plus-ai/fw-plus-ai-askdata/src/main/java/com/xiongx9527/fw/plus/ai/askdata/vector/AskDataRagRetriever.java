package com.xiongx9527.fw.plus.ai.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import java.util.List;

/** 基于向量存储的 RAG 检索器。 */
public final class AskDataRagRetriever {
	private final AskDataVectorStore store;
	public AskDataRagRetriever(AskDataVectorStore store) { if (store == null) throw new IllegalArgumentException("store 不能为空"); this.store = store; }
	public List<AskDataVectorDocument> retrieve(AskDataProjectContext context, AskDataVectorSearchRequest request) { return store.search(context, request); }
}
