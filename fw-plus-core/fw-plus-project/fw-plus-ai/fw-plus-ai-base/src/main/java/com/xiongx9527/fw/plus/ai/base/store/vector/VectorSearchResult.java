package com.xiongx9527.fw.plus.ai.base.store.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 向量检索响应：命中列表（按分数降序）。
 *
 * <p>用响应对象而不是裸 {@code List}：后续要加「实际检索的候选数」「后端耗时」
 * 「是否降级」这类字段时，不必再改所有实现与调用方。</p>
 */
public final class VectorSearchResult {

    private static final VectorSearchResult EMPTY =
            new VectorSearchResult(Collections.<ScoredDocument>emptyList());

    private final List<ScoredDocument> hits;

    private VectorSearchResult(List<ScoredDocument> hits) {
        this.hits = Collections.unmodifiableList(new ArrayList<ScoredDocument>(hits));
    }

    public static VectorSearchResult of(List<ScoredDocument> hits) {
        if (hits == null || hits.isEmpty()) {
            return EMPTY;
        }
        return new VectorSearchResult(hits);
    }

    public static VectorSearchResult empty() {
        return EMPTY;
    }

    public List<ScoredDocument> getHits() {
        return hits;
    }

    public boolean isEmpty() {
        return hits.isEmpty();
    }

    public int size() {
        return hits.size();
    }
}
