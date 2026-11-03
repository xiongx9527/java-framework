package com.xiongx9527.fw.plus.ai.support.cache;

import com.xiongx9527.fw.plus.ai.base.cache.CachePort;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import java.util.ArrayList;
import java.util.List;

/**
 * 给任意 {@link Retriever} 加缓存：相同查询（文本 + 集合 + topK）命中时直接返回旧结果。
 *
 * <p>缓存键拼入 {@link CachePort#epoch()}，数据变更后自动失效。</p>
 */
public final class CachingRetriever<T> implements Retriever<T> {

    private final Retriever<T> delegate;

    private final CachePort cache;

    private final long ttlMillis;

    public CachingRetriever(Retriever<T> delegate, CachePort cache, long ttlMillis) {
        this.delegate = delegate;
        this.cache = cache;
        this.ttlMillis = ttlMillis <= 0 ? 3_600_000L : ttlMillis;
    }

    @Override
    public List<Retrieved<T>> retrieve(RetrievalQuery query) {
        if (query == null) {
            return delegate.retrieve(null);
        }
        String key = "ret:" + cache.epoch() + ":" + query.getCollection() + ":" + query.getText()
                + ":" + query.getTopK();
        Object hit = cache.get(key);
        if (hit instanceof List) {
            @SuppressWarnings("unchecked")
            List<Retrieved<T>> cached = (List<Retrieved<T>>) hit;
            return new ArrayList<Retrieved<T>>(cached);
        }
        List<Retrieved<T>> result = delegate.retrieve(query);
        cache.put(key, new ArrayList<Retrieved<T>>(result), ttlMillis);
        return result;
    }
}
