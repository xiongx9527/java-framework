package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全文检索：query 与文档内容的 TF-IDF 余弦相似度（0~1，Dify 式）。
 *
 * <p>这是「对文本建倒排索引后按词频打分」，与「关键字命中业务字段」（见 support 的
 * {@code KeywordRetriever}）是两回事：前者看整段文本的相似度，后者看抽取出的关键词
 * 是否命中表名 / 注释等结构化字段。</p>
 *
 * <p>分数与向量余弦同量纲（都是 0~1 的余弦），因此混合加权无需额外归一化。</p>
 */
public class FullTextRetriever implements DocumentRetriever {

    @Override
    public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
        List<Document> corpus = query == null ? null : query.getCorpus();
        if (corpus == null || corpus.isEmpty() || query.getText() == null || query.getText().trim().isEmpty()) {
            return new ArrayList<Retrieved<Document>>();
        }
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

        List<Retrieved<Document>> scored = new ArrayList<Retrieved<Document>>();
        for (int i = 0; i < corpus.size(); i++) {
            double sim = cosine(queryVec, tfidf(docTokens.get(i), df, n));
            if (sim > 0.0) {
                scored.add(Retrieved.of(corpus.get(i), sim, "full-text"));
            }
        }
        scored.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        int topK = query.getTopK();
        if (topK > 0 && scored.size() > topK) {
            return new ArrayList<Retrieved<Document>>(scored.subList(0, topK));
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
