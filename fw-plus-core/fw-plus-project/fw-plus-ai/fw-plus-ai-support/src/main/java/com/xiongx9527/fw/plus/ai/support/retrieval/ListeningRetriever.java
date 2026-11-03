package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 监听装饰器：把查询 / 命中 / 失败事件派发给 {@link RetrievalListener}。
 */
public class ListeningRetriever<T> implements Retriever<T> {

    private final Retriever<T> delegate;

    private final List<RetrievalListener> listeners;

    public ListeningRetriever(Retriever<T> delegate, List<RetrievalListener> listeners) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate retriever is required");
        }
        this.delegate = delegate;
        this.listeners = listeners == null ? new ArrayList<RetrievalListener>() : listeners;
    }

    @Override
    public List<Retrieved<T>> retrieve(RetrievalQuery query) {
        for (RetrievalListener listener : listeners) {
            try {
                listener.onQuery(query);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
        List<Retrieved<T>> hits;
        try {
            hits = delegate.retrieve(query);
        } catch (RuntimeException e) {
            for (RetrievalListener listener : listeners) {
                try {
                    listener.onError(e.getMessage());
                } catch (RuntimeException ignored) {
                    // 监听器异常不得影响主链路
                }
            }
            throw e;
        }
        for (RetrievalListener listener : listeners) {
            try {
                listener.onHits(hits);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
        return hits;
    }
}
