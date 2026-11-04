package com.xiongx9527.fw.plus.ai.base.retrieval;

import java.util.List;

/**
 * Default RetrievalCapability backed by a Retriever<T>.
 */
public class DefaultRetrievalCapability<T> implements RetrievalCapability<T> {

    private final Retriever<T> retriever;

    public DefaultRetrievalCapability(Retriever<T> retriever) {
        if (retriever == null) {
            throw new IllegalArgumentException("retriever is required");
        }
        this.retriever = retriever;
    }

    @Override
    public RetrievalResult<T> retrieve(RetrievalQuery query) {
        List<Retrieved<T>> hits = retriever.retrieve(query);
        return RetrievalResult.<T>builder().query(query).hits(hits).build();
    }
}
