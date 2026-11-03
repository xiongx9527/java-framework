package com.xiongx9527.fw.plus.ai.base.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量模型端口：把文本编码为稠密向量。
 *
 * <p>向量以 {@code double[]} 交付，与向量存储端口的入参一致，避免在召回链路上做数值类型转换。</p>
 */
public interface EmbeddingPort {

    /** 编码单条文本；失败返回 {@code null} 表示该条不可用（调用方据此降级）。 */
    double[] embed(String text);

    /** 批量编码，返回顺序与入参一一对应。默认逐条编码，后端有批量接口时应覆写。 */
    default List<double[]> embedAll(List<String> texts) {
        List<double[]> out = new ArrayList<double[]>();
        if (texts == null) {
            return out;
        }
        for (String text : texts) {
            out.add(embed(text));
        }
        return out;
    }

    /** 向量维度；未知时返回 {@code 0}（用于与向量集合维度做联合校验）。 */
    default int dimension() {
        return 0;
    }
}
