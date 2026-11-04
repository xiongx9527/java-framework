package com.xiongx9527.fw.plus.ai.base.store.vector;

/**
 * 检索命中：文档 + 相关度分数（0~1，越大越相关）。
 */
public final class ScoredDocument {

    private final Document document;

    private final double score;

    public ScoredDocument(Document document, double score) {
        this.document = document;
        this.score = score;
    }

    public Document getDocument() {
        return document;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "ScoredDocument{id=" + (document == null ? null : document.getId()) + ", score=" + score + '}';
    }
}
