package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;

/**
 * 文档检索器：条目类型固定为 {@link Document} 的常用特化。
 *
 * <p>单独成接口只是为了让调用方少写泛型参数；语义与 {@code Retriever<Document>} 完全一致。</p>
 */
public interface DocumentRetriever extends Retriever<Document> {
}
