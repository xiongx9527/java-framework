package com.xiongx9527.fw.plus.ai.support.retrieval;

import lombok.Getter;

/** 检索结果：文档 + 相关度分数（0~1）。 */
@Getter
public final class ScoredDocument {
    private final Document document;
    private final double score;

    public ScoredDocument(Document document, double score) {
        this.document = document;
        this.score = score;
    }
}
