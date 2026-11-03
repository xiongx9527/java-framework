package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * 排序 + 截断装饰器：按分数降序排序后取前 {@link RetrievalQuery#getTopN()} 条。
 */
public class TopNRetriever<T> implements Retriever<T> {

    private final Retriever<T> delegate;

    public TopNRetriever(Retriever<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate retriever is required");
        }
        this.delegate = delegate;
    }

    @Override
    public List<Retrieved<T>> retrieve(RetrievalQuery query) {
        List<Retrieved<T>> hits = delegate.retrieve(query);
        List<Retrieved<T>> sorted = new ArrayList<Retrieved<T>>();
        if (hits != null) {
            sorted.addAll(hits);
        }
        sorted.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        int topN = query == null ? 0 : query.getTopN();
        if (topN > 0 && sorted.size() > topN) {
            return new ArrayList<Retrieved<T>>(sorted.subList(0, topN));
        }
        return sorted;
    }
}
