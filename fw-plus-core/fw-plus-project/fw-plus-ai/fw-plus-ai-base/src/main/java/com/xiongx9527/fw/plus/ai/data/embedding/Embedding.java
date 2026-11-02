package com.xiongx9527.fw.plus.ai.data.embedding;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量：浮点数组 + 维度。
 *
 * <p><b>作用</b>：嵌入模型输出、向量库写入与检索的统一载体。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.embedding.Embedding}
 * （{@code vector()/vectorAsList()/vectorAsDoubleArray()/normalize()/dimension()/from(...)}）。</p>
 *
 * <p><b>实现约定</b>：{@link #vector()} 返回内部数组引用（与官方一致，避免大数组复制）；
 * 调用方不应修改它。</p>
 *
 * @author xiongxiao
 */
public class Embedding {

    private final float[] vector;

    /**
     * 构造向量。
     *
     * @param vector 向量分量
     */
    public Embedding(float[] vector) {
        this.vector = vector == null ? new float[0] : vector;
    }

    /**
     * 向量分量。
     *
     * @return 内部数组引用
     */
    public float[] vector() {
        return vector;
    }

    /**
     * 向量分量（装箱列表）。
     *
     * @return 只读列表
     */
    public List<Float> vectorAsList() {
        List<Float> list = new ArrayList<Float>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }

    /**
     * 向量分量（双精度）。
     *
     * @return 新数组
     */
    public double[] vectorAsDoubleArray() {
        double[] doubles = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            doubles[i] = vector[i];
        }
        return doubles;
    }

    /**
     * 原地做 L2 归一化。
     */
    public void normalize() {
        double norm = 0.0d;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0.0d) {
            return;
        }
        float length = (float) Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / length;
        }
    }

    /**
     * 维度。
     *
     * @return 分量个数
     */
    public int dimension() {
        return vector.length;
    }

    @Override
    public String toString() {
        return "Embedding{dimension=" + vector.length + '}';
    }

    /**
     * 由 float 数组创建。
     *
     * @param vector 向量分量
     * @return 向量
     */
    public static Embedding from(float[] vector) {
        return new Embedding(vector);
    }

    /**
     * 由装箱列表创建。
     *
     * @param vector 向量分量
     * @return 向量
     */
    public static Embedding from(List<Float> vector) {
        float[] floats = new float[vector == null ? 0 : vector.size()];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = vector.get(i);
        }
        return new Embedding(floats);
    }
}
