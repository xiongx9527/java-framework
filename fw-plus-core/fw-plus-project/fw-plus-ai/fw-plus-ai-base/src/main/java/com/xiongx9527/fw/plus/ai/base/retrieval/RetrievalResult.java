package com.xiongx9527.fw.plus.ai.base.retrieval;

import java.util.Collections;
import java.util.List;

/**
 * Typed retrieval result: the query plus the ordered hits.
 */
public final class RetrievalResult<T> {

    private final RetrievalQuery query;
    private final List<Retrieved<T>> hits;

    private RetrievalResult(Builder<T> builder) {
        this.query = builder.query;
        this.hits = Collections.unmodifiableList(builder.hits);
    }

    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    public RetrievalQuery getQuery() {
        return query;
    }

    public List<Retrieved<T>> getHits() {
        return hits;
    }

    public static final class Builder<T> {
        private RetrievalQuery query;
        private List<Retrieved<T>> hits = Collections.emptyList();

        public Builder<T> query(RetrievalQuery value) {
            this.query = value;
            return this;
        }

        public Builder<T> hits(List<Retrieved<T>> value) {
            this.hits = value == null ? Collections.emptyList() : value;
            return this;
        }

        public RetrievalResult<T> build() {
            return new RetrievalResult<T>(this);
        }
    }
}
