package com.xiongx9527.fw.plus.ai.support.rag;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.List;

/**
 * 默认 RAG 编排实现，{@link RagAugmentor} 的直接落地类。
 *
 * <p>继承旧的 {@link DefaultRetrievalAugmentor} 以复用全部编排逻辑，
 * 从而消除同名类带来的二义性而不改动既有行为。</p>
 */
@SuppressWarnings("deprecation")
public class DefaultRagAugmentor extends DefaultRetrievalAugmentor {

    public DefaultRagAugmentor(List<Retriever<Document>> retrievers) {
        super(retrievers);
    }

    public DefaultRagAugmentor(List<Retriever<Document>> retrievers, ContentAggregator aggregator,
                                ContentInjector injector, QueryTransformer transformer, QueryRouter router) {
        super(retrievers, aggregator, injector, transformer, router);
    }
}
