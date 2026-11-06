package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;

/**
 * Web / 搜索引擎检索器的具名 seam（接缝）。
 *
 * <p>语义与 {@link DocumentRetriever} 完全一致（条目固定为 {@link Document}、返回带分数结果），
 * 单独成接口只是为了让组合层（{@code support.retrieval.RetrievalStrategies}）能把 web 检索器
 * 与向量 / 词法检索器区分开，作为一路独立的检索来源参与融合。</p>
 *
 * <p>langchain4j 的 web / search-engine 检索实现只要实现本接口，
 * 即可无缝接入 {@code RetrievalStrategies} 的融合链路，框架不需要再定义新的契约：
 * 知识库向量检索、词法检索、web 检索都统一复用 {@link Retriever} 这一条契约。</p>
 */
public interface WebRetriever extends DocumentRetriever {
}
