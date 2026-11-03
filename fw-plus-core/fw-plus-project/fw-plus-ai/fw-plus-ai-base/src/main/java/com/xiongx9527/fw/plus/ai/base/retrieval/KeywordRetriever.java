package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.Document;
import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 词法检索：默认用 query 与文档内容的 TF-IDF 余弦相似度（0~1，Dify 式），
 * 与向量余弦同量纲，因此混合加权无需额外归一化。
 *
 * <p>构造时注入 {@link LexicalScorer} 可替换打分口径（例如表结构召回用规则加权），
 * 检索链路（排序、截断、阈值）保持不变。</p>
 */
public class KeywordRetriever implements Retriever {

    private final LexicalScorer scorer;

    public KeywordRetriever() {
        this(null);
    }

    public KeywordRetriever(LexicalScorer scorer) {
        this.scorer = scorer;
    }

    @Override
    public List<ScoredDocument> retrieve(RetrievalQuery query) {
        List<Document> corpus = query == null ? null : query.getCorpus();
        if (corpus == null || corpus.isEmpty() || query.getText() == null || query.getText().trim().isEmpty()) {
            return new ArrayList<ScoredDocument>();
        }
        List<ScoredDocument> scored = scorer != null
                ? scoreWithPort(query, corpus)
                : scoreWithTfidf(query, corpus);
        scored.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        int topK = query.getTopK();
        if (topK > 0 && scored.size() > topK) {
            return new ArrayList<ScoredDocument>(scored.subList(0, topK));
        }
        return scored;
    }

    private List<ScoredDocument> scoreWithPort(RetrievalQuery query, List<Document> corpus) {
        List<ScoredDocument> scored = new ArrayList<ScoredDocument>();
        for (Document doc : corpus) {
            double sim = scorer.score(query.getText(), doc);
            if (sim > 0.0) {
                scored.add(new ScoredDocument(doc, sim));
            }
        }
        return scored;
    }

    private List<ScoredDocument> scoreWithTfidf(RetrievalQuery query, List<Document> corpus) {
        List<List<String>> docTokens = new ArrayList<List<String>>();
        Map<String, Integer> df = new HashMap<String, Integer>();
        for (Document doc : corpus) {
            List<String> tokens = TextTokenizer.tokenize(doc.getContent());
            docTokens.add(tokens);
            for (String t : new HashSet<String>(tokens)) {
                df.merge(t, 1, Integer::sum);
            }
        }
        int n = corpus.size();
        Map<String, Double> queryVec = tfidf(TextTokenizer.tokenize(query.getText()), df, n);

        List<ScoredDocument> scored = new ArrayList<ScoredDocument>();
        for (int i = 0; i < corpus.size(); i++) {
            double sim = cosine(queryVec, tfidf(docTokens.get(i), df, n));
            if (sim > 0.0) {
                scored.add(new ScoredDocument(corpus.get(i), sim));
            }
        }
        return scored;
    }

    private Map<String, Double> tfidf(List<String> tokens, Map<String, Integer> df, int n) {
        Map<String, Integer> tf = new LinkedHashMap<String, Integer>();
        for (String t : tokens) {
            tf.merge(t, 1, Integer::sum);
        }
        Map<String, Double> vec = new HashMap<String, Double>();
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            int d = df.getOrDefault(e.getKey(), 0);
            double idf = Math.log((1.0 + n) / (1.0 + d)) + 1.0;
            vec.put(e.getKey(), e.getValue() * idf);
        }
        return vec;
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0.0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double bv = b.get(e.getKey());
            if (bv != null) {
                dot += e.getValue() * bv;
            }
        }
        double na = norm(a);
        double nb = norm(b);
        if (na == 0.0 || nb == 0.0) {
            return 0.0;
        }
        return dot / (na * nb);
    }

    private double norm(Map<String, Double> v) {
        double sum = 0.0;
        for (Double x : v.values()) {
            sum += x * x;
        }
        return Math.sqrt(sum);
    }
}
