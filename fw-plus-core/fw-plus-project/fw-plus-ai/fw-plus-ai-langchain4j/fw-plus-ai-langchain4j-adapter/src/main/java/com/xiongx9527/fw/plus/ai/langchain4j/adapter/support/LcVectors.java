package com.xiongx9527.fw.plus.ai.langchain4j.adapter.support;

import dev.langchain4j.data.embedding.Embedding;

/** base 用 {@code double[]}、LangChain4j 用 {@code float[]} 承载向量，这里做双向转换。 */
public final class LcVectors {

    private LcVectors() {
    }

    public static float[] toFloat(double[] vector) {
        if (vector == null) {
            return null;
        }
        float[] out = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            out[i] = (float) vector[i];
        }
        return out;
    }

    public static double[] toDouble(float[] vector) {
        if (vector == null) {
            return null;
        }
        double[] out = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            out[i] = vector[i];
        }
        return out;
    }

    public static Embedding toEmbedding(double[] vector) {
        float[] floats = toFloat(vector);
        return floats == null ? null : Embedding.from(floats);
    }

    public static double[] toDouble(Embedding embedding) {
        return embedding == null ? null : toDouble(embedding.vector());
    }
}
