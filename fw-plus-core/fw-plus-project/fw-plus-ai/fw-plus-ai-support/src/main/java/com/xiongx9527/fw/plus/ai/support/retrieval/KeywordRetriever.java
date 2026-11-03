package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 词法检索：query 与文档内容的 TF-IDF 余弦相似度（0~1，Dify 式）。
 * 与向量余弦同量纲，因此混合加权无需额外归一化。
 */
public class KeywordRetriever implements Retriever {

    @Override
    public List<ScoredDocument> retrieve(RetrievalQuery query) {
        List<Document> corpus = query == null ? null : query.getCorpus();
        if (corpus == null || corpus.isEmpty() || query.getText() == null || query.getText().trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<List<String>> docTokens = new ArrayList<>();
        Map<String, Integer> df = new HashMap<>();
        for (Document doc : corpus) {
            List<String> tokens = TextTokenizer.tokenize(doc.getContent());
            docTokens.add(tokens);
            for (String t : new HashSet<>(tokens)) {
                df.merge(t, 1, Integer::sum);
            }
        }
        int n = corpus.size();
        Map<String, Double> queryVec = tfidf(TextTokenizer.tokenize(query.getText()), df, n);

        List<ScoredDocument> scored = new ArrayList<>();
        for (int i = 0; i < corpus.size(); i++) {
            double sim = cosine(queryVec, tfidf(docTokens.get(i), df, n));
            if (sim > 0.0) {
                scored.add(new ScoredDocument(corpus.get(i), sim));
            }
        }
        scored.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        int topK = query.getTopK();
        if (topK > 0 && scored.size() > topK) {
            return new ArrayList<>(scored.subList(0, topK));
        }
        return scored;
    }

    private Map<String, Double> tfidf(List<String> tokens, Map<String, Integer> df, int n) {
        Map<String, Integer> tf = new LinkedHashMap<>();
        for (String t : tokens) {
            tf.merge(t, 1, Integer::sum);
        }
        Map<String, Double> vec = new HashMap<>();
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
