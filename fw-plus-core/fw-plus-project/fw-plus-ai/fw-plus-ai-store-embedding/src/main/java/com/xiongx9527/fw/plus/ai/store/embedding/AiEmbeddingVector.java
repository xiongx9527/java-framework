package com.xiongx9527.fw.plus.ai.store.embedding;

public final class AiEmbeddingVector {

    private final float[] values;

    public AiEmbeddingVector(float[] values) {
        this.values = values == null ? new float[0] : values.clone();
    }

    public int dimension() {
        return values.length;
    }

    public float[] toArray() {
        return values.clone();
    }
}
