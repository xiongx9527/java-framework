package com.xiongx9527.fw.plus.ai.rag.aggregator;

import com.xiongx9527.fw.plus.ai.rag.content.Content;
import com.xiongx9527.fw.plus.ai.rag.content.ContentMetadata;
import com.xiongx9527.fw.plus.ai.model.output.Response;
import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.model.scoring.ScoringModel;
import com.xiongx9527.fw.plus.ai.retriever.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 基于 {@link ScoringModel} 的重排聚合器，镜像 langchain4j 1.23.0 的
 * {@code dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator}（fuse + reRankAndFilter + builder）。
 */
public class ReRankingContentAggregator implements ContentAggregator {

    public static final Function<Map<Query, Collection<List<Content>>>, Query> DEFAULT_QUERY_SELECTOR =
            contents -> (contents == null || contents.isEmpty()) ? null : contents.keySet().iterator().next();

    private final ScoringModel scoringModel;
    private final Function<Map<Query, Collection<List<Content>>>, Query> querySelector;
    private final Double minScore;
    private final Integer maxResults;

    public ReRankingContentAggregator(ScoringModel scoringModel) {
        this(scoringModel, DEFAULT_QUERY_SELECTOR, null, null);
    }

    public ReRankingContentAggregator(ScoringModel scoringModel,
                                      Function<Map<Query, Collection<List<Content>>>, Query> querySelector,
                                      Double minScore) {
        this(scoringModel, querySelector, minScore, null);
    }

    public ReRankingContentAggregator(ScoringModel scoringModel,
                                      Function<Map<Query, Collection<List<Content>>>, Query> querySelector,
                                      Double minScore,
                                      Integer maxResults) {
        this.scoringModel = scoringModel;
        this.querySelector = querySelector == null ? DEFAULT_QUERY_SELECTOR : querySelector;
        this.minScore = minScore;
        this.maxResults = maxResults;
    }

    @Override
    public List<Content> aggregate(Map<Query, Collection<List<Content>>> contents) {
        Map<Query, List<Content>> fused = fuse(contents);
        if (fused.isEmpty()) return Collections.emptyList();
        Query query = querySelector.apply(contents);
        List<Content> all = new ArrayList<>();
        for (List<Content> list : fused.values()) all.addAll(list);
        return reRankAndFilter(all, query);
    }

    protected Map<Query, List<Content>> fuse(Map<Query, Collection<List<Content>>> contents) {
        Map<Query, List<Content>> fused = new LinkedHashMap<>();
        if (contents != null) {
            for (Map.Entry<Query, Collection<List<Content>>> e : contents.entrySet()) {
                List<Content> flat = new ArrayList<>();
                if (e.getValue() != null) {
                    for (List<Content> l : e.getValue()) if (l != null) flat.addAll(l);
                }
                fused.put(e.getKey(), flat);
            }
        }
        return fused;
    }

    protected List<Content> reRankAndFilter(List<Content> contents, Query query) {
        if (contents == null || contents.isEmpty() || query == null) return Collections.emptyList();
        List<TextSegment> segments = new ArrayList<>();
        for (Content c : contents) segments.add(c.textSegment());
        Response<List<Double>> response = scoringModel.scoreAll(segments, query.text());
        List<Double> scores = response == null ? null : response.content();
        List<ScoredContent> scored = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            Double s = (scores != null && i < scores.size()) ? scores.get(i) : null;
            if (s == null) continue;
            if (minScore != null && s < minScore) continue;
            Map<ContentMetadata, Object> metadata = new EnumMap<ContentMetadata, Object>(ContentMetadata.class);
            metadata.putAll(contents.get(i).metadata());
            metadata.put(ContentMetadata.RERANKED_SCORE, s);
            scored.add(new ScoredContent(Content.from(contents.get(i).textSegment(), metadata), s));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        if (maxResults != null && scored.size() > maxResults) {
            scored = new ArrayList<>(scored.subList(0, maxResults));
        }
        List<Content> out = new ArrayList<>();
        for (ScoredContent sc : scored) out.add(sc.content);
        return out;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private ScoringModel scoringModel;
        private Function<Map<Query, Collection<List<Content>>>, Query> querySelector = DEFAULT_QUERY_SELECTOR;
        private Double minScore;
        private Integer maxResults;
        public Builder scoringModel(ScoringModel scoringModel) { this.scoringModel = scoringModel; return this; }
        public Builder querySelector(Function<Map<Query, Collection<List<Content>>>, Query> querySelector) { this.querySelector = querySelector; return this; }
        public Builder minScore(Double minScore) { this.minScore = minScore; return this; }
        public Builder maxResults(Integer maxResults) { this.maxResults = maxResults; return this; }
        public ReRankingContentAggregator build() { return new ReRankingContentAggregator(scoringModel, querySelector, minScore, maxResults); }
    }

    private static class ScoredContent {
        final Content content;
        final double score;
        ScoredContent(Content content, double score) { this.content = content; this.score = score; }
    }
}
