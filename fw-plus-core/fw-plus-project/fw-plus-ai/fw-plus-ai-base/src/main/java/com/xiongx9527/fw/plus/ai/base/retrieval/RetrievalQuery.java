package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 检索请求。
 *
 * <p>{@code topK} 是每路召回上限，{@code topN} 是最终返回上限；
 * {@code scoreThreshold} 作用在融合 / 重排后的最终分上（对齐 Dify）。
 * {@code corpus} 只对词法检索有意义（向量检索由向量库承担候选集）。</p>
 */
public final class RetrievalQuery {

    private final String text;

    private final String collection;

    private final int topK;

    private final int topN;

    private final double scoreThreshold;

    private final Map<String, Object> filters;

    private final Set<String> candidateIds;

    private final List<Document> corpus;

    private RetrievalQuery(Builder builder) {
        this.text = builder.text;
        this.collection = builder.collection;
        this.topK = builder.topK;
        this.topN = builder.topN;
        this.scoreThreshold = builder.scoreThreshold;
        this.filters = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.filters));
        this.candidateIds = Collections.unmodifiableSet(new LinkedHashSet<String>(builder.candidateIds));
        this.corpus = Collections.unmodifiableList(new java.util.ArrayList<Document>(builder.corpus));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getText() {
        return text;
    }

    /** 向量检索使用的集合名；词法检索忽略。 */
    public String getCollection() {
        return collection;
    }

    public int getTopK() {
        return topK;
    }

    public int getTopN() {
        return topN;
    }

    public double getScoreThreshold() {
        return scoreThreshold;
    }

    /** 元数据等值过滤（向量检索用）。 */
    public Map<String, Object> getFilters() {
        return filters;
    }

    /** 候选 id 限定（向量检索用）；空集表示不限定。 */
    public Set<String> getCandidateIds() {
        return candidateIds;
    }

    /** 词法检索的候选文档集合。 */
    public List<Document> getCorpus() {
        return corpus;
    }

    public static final class Builder {

        private final Map<String, Object> filters = new LinkedHashMap<String, Object>();

        private final Set<String> candidateIds = new LinkedHashSet<String>();

        private final List<Document> corpus = new java.util.ArrayList<Document>();

        private String text;

        private String collection;

        private int topK = 10;

        private int topN = 8;

        private double scoreThreshold = 0.0;

        private Builder() {
        }

        public Builder text(String value) {
            this.text = value;
            return this;
        }

        public Builder collection(String value) {
            this.collection = value;
            return this;
        }

        public Builder topK(int value) {
            this.topK = value;
            return this;
        }

        public Builder topN(int value) {
            this.topN = value;
            return this;
        }

        public Builder scoreThreshold(double value) {
            this.scoreThreshold = value;
            return this;
        }

        public Builder filters(Map<String, Object> value) {
            this.filters.clear();
            if (value != null) {
                this.filters.putAll(value);
            }
            return this;
        }

        public Builder filter(String key, Object value) {
            if (key != null) {
                this.filters.put(key, value);
            }
            return this;
        }

        public Builder candidateIds(Collection<String> value) {
            this.candidateIds.clear();
            if (value != null) {
                this.candidateIds.addAll(value);
            }
            return this;
        }

        public Builder corpus(List<Document> value) {
            this.corpus.clear();
            if (value != null) {
                this.corpus.addAll(value);
            }
            return this;
        }

        public RetrievalQuery build() {
            return new RetrievalQuery(this);
        }
    }
}
