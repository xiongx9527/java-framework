package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.DocumentRetriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 关键字检索：从问题抽取关键词，按 {@link KeywordScorer} 在候选文档上打分。
 *
 * <p>与 base 的 {@code FullTextRetriever} 是两条不同的路：全文检索对整段文本做词频相似度，
 * 关键字检索只关心「抽出来的词有没有命中业务字段」。表结构召回这类场景用后者更准。</p>
 */
public class KeywordRetriever implements DocumentRetriever {

    private final KeywordScorer scorer;

    private final KeywordExtractor extractor;

    public KeywordRetriever(KeywordScorer scorer) {
        this(scorer, KeywordRetriever::split);
    }

    public KeywordRetriever(KeywordScorer scorer, KeywordExtractor extractor) {
        if (scorer == null) {
            throw new IllegalArgumentException("keyword scorer is required");
        }
        this.scorer = scorer;
        this.extractor = extractor == null ? KeywordRetriever::split : extractor;
    }

    @Override
    public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
        List<Retrieved<Document>> result = new ArrayList<Retrieved<Document>>();
        if (query == null || query.getCorpus() == null || query.getCorpus().isEmpty()) {
            return result;
        }
        List<String> keywords = extractor.extract(query.getText());
        if (keywords == null || keywords.isEmpty()) {
            return result;
        }
        for (Document document : query.getCorpus()) {
            double score = scorer.score(keywords, document);
            if (score > 0.0) {
                result.add(Retrieved.of(document, score, "keyword"));
            }
        }
        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        int topK = query.getTopK();
        if (topK > 0 && result.size() > topK) {
            return new ArrayList<Retrieved<Document>>(result.subList(0, topK));
        }
        return result;
    }

    /** 默认抽取：按非字母数字切分，去重保序。 */
    static List<String> split(String query) {
        List<String> keywords = new ArrayList<String>();
        if (query == null) {
            return keywords;
        }
        for (String part : query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (!part.isEmpty() && !keywords.contains(part)) {
                keywords.add(part);
            }
        }
        return keywords;
    }
}
