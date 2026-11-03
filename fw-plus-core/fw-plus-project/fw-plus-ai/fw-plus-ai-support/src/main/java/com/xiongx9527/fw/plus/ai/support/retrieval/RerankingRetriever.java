package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Reranker;
import java.util.ArrayList;
import java.util.List;

/**
 * 重排装饰器：检索后接一次重排。
 */
public class RerankingRetriever<T> implements Retriever<T> {

    private final Retriever<T> delegate;

    private final Reranker<T> reranker;

    public RerankingRetriever(Retriever<T> delegate, Reranker<T> reranker) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate retriever is required");
        }
        if (reranker == null) {
            throw new IllegalArgumentException("reranker is required");
        }
        this.delegate = delegate;
        this.reranker = reranker;
    }

    @Override
    public List<Retrieved<T>> retrieve(RetrievalQuery query) {
        List<Retrieved<T>> candidates = delegate.retrieve(query);
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<Retrieved<T>>();
        }
        List<Retrieved<T>> reranked = reranker.rerank(query == null ? null : query.getText(), candidates);
        return reranked == null ? candidates : reranked;
    }
}
