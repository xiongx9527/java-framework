package com.xiongx9527.fw.plus.ai.store.embedding;

public final class AiEmbeddingMatch {

    private final double score;

    private final String embeddingId;

    private final AiEmbeddingDocument document;

    public AiEmbeddingMatch(double score, String embeddingId, AiEmbeddingDocument document) {
        this.score = score;
        this.embeddingId = embeddingId;
        this.document = document;
    }

    public double getScore() {
        return score;
    }

    public String getEmbeddingId() {
        return embeddingId;
    }

    public AiEmbeddingDocument getDocument() {
        return document;
    }
}
