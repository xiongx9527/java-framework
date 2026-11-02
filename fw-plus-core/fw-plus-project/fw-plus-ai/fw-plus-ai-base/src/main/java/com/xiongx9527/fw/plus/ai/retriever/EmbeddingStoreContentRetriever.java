package com.xiongx9527.fw.plus.ai.retriever;

import com.xiongx9527.fw.plus.ai.model.output.Response;
import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;
import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.model.embedding.EmbeddingModel;
import com.xiongx9527.fw.plus.ai.rag.content.Content;
import com.xiongx9527.fw.plus.ai.rag.content.ContentMetadata;
import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingSearchRequest;
import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingSearchResult;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 {@link EmbeddingStore} + {@link EmbeddingModel} 的通用检索实现。
 *
 * <p><b>作用</b>：把「查询向量化 → 向量库召回 → 包装成内容」这条最常见的 RAG 路径固化下来，
 * 换向量库只换 {@link EmbeddingStore} 实现，换模型只换 {@link EmbeddingModel} 实现。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever}
 * （经 {@code EmbeddingSearchRequest} 检索，召回结果写入 {@link ContentMetadata#SCORE}
 * 与 {@link ContentMetadata#EMBEDDING_ID}）。</p>
 *
 * <p><b>实现约定</b>：线程安全；召回为空时返回空列表。</p>
 *
 * @author xiongxiao
 */
public class EmbeddingStoreContentRetriever implements ContentRetriever {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final int maxResults;
    private final double minScore;

    /**
     * 构造检索器。
     *
     * @param embeddingStore 向量库
     * @param embeddingModel 嵌入模型
     * @param maxResults     最大召回条数
     * @param minScore       最低相似度
     */
    public EmbeddingStoreContentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel,
                                          int maxResults, double minScore) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    @Override
    public List<Content> retrieve(Query query) {
        Response<Embedding> reference = embeddingModel.embed(query.text());
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .query(query.text())
                .queryEmbedding(reference.content())
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();
        List<Content> contents = new ArrayList<Content>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            if (segment == null) {
                continue;
            }
            Map<ContentMetadata, Object> metadata = new EnumMap<ContentMetadata, Object>(ContentMetadata.class);
            if (match.score() != null) {
                metadata.put(ContentMetadata.SCORE, match.score());
            }
            if (match.embeddingId() != null) {
                metadata.put(ContentMetadata.EMBEDDING_ID, match.embeddingId());
            }
            contents.add(Content.from(segment, metadata));
        }
        return contents;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link EmbeddingStoreContentRetriever} 构建器。 */
    public static final class Builder {
        private EmbeddingStore<TextSegment> embeddingStore;
        private EmbeddingModel embeddingModel;
        private int maxResults = 3;
        private double minScore = 0.0d;

        public Builder embeddingStore(EmbeddingStore<TextSegment> embeddingStore) { this.embeddingStore = embeddingStore; return this; }
        public Builder embeddingModel(EmbeddingModel embeddingModel) { this.embeddingModel = embeddingModel; return this; }
        public Builder maxResults(int maxResults) { this.maxResults = maxResults; return this; }
        public Builder minScore(double minScore) { this.minScore = minScore; return this; }
        public EmbeddingStoreContentRetriever build() {
            return new EmbeddingStoreContentRetriever(embeddingStore, embeddingModel, maxResults, minScore);
        }
    }
}
