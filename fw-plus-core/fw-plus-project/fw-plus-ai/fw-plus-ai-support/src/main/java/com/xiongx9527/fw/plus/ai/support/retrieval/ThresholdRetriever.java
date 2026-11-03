package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * 阈值装饰器：丢弃分数低于 {@link RetrievalQuery#getScoreThreshold()} 的条目。
 *
 * <p>阈值 {@code <= 0} 表示不过滤；过滤在重排之后、截断之前，与 Dify 一致。</p>
 */
public class ThresholdRetriever<T> implements Retriever<T> {

    private final Retriever<T> delegate;

    public ThresholdRetriever(Retriever<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate retriever is required");
        }
        this.delegate = delegate;
    }

    @Override
    public List<Retrieved<T>> retrieve(RetrievalQuery query) {
        List<Retrieved<T>> hits = delegate.retrieve(query);
        if (hits == null || hits.isEmpty()) {
            return new ArrayList<Retrieved<T>>();
        }
        double threshold = query == null ? 0.0 : query.getScoreThreshold();
        if (threshold <= 0.0) {
            return hits;
        }
        List<Retrieved<T>> filtered = new ArrayList<Retrieved<T>>();
        for (Retrieved<T> hit : hits) {
            if (hit != null && hit.getScore() >= threshold) {
                filtered.add(hit);
            }
        }
        return filtered;
    }
}
