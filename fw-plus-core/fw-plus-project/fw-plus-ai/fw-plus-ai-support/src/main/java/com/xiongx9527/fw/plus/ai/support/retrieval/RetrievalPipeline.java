package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Reranker;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalListener;
import java.util.List;

/**
 * 标准检索装配：检索 → 可选重排 → 阈值过滤 → 排序 + topN → 可选监听。
 *
 * <p>它就是几个装饰器的固定组合，本身没有额外逻辑；需要非标准顺序时直接自己叠装饰器。</p>
 *
 * @param <T> 条目类型
 */
public final class RetrievalPipeline<T> implements Retriever<T> {

    private final Retriever<T> delegate;

    private RetrievalPipeline(Retriever<T> delegate) {
        this.delegate = delegate;
    }

    public static <T> Retriever<T> of(Retriever<T> retriever) {
        return of(retriever, null, null);
    }

    public static <T> Retriever<T> of(Retriever<T> retriever, Reranker<T> reranker) {
        return of(retriever, reranker, null);
    }

    public static <T> Retriever<T> of(Retriever<T> retriever, Reranker<T> reranker, List<RetrievalListener> listeners) {
        if (retriever == null) {
            throw new IllegalArgumentException("retriever is required");
        }
        Retriever<T> chain = retriever;
        if (reranker != null) {
            chain = new RerankingRetriever<T>(chain, reranker);
        }
        chain = new ThresholdRetriever<T>(chain);
        chain = new TopNRetriever<T>(chain);
        if (listeners != null && !listeners.isEmpty()) {
            chain = new ListeningRetriever<T>(chain, listeners);
        }
        return new RetrievalPipeline<T>(chain);
    }

    @Override
    public List<Retrieved<T>> retrieve(RetrievalQuery query) {
        return delegate.retrieve(query);
    }
}
