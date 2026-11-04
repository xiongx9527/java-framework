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

    /**
     * 枚举该后端可用的向量模型，供配置页选型使用。
     *
     * <p>不支持枚举的后端返回空列表（不抛异常）；列表项的 {@link ModelInfo#getType()} 为
     * {@link ModelType#EMBEDDING}，已知维度时一并填入 {@link ModelInfo#getDimension()}。</p>
     */
    default List<ModelInfo> listModels() {
        return ModelCatalogSupport.empty();
    }

    /**
     * 校验给定模型是否可用：默认用当前实例真实编码一条探针文本，能拿到非空向量即视为可用
     * （地址、鉴权、模型名同时被验证）。需要按模型名切换后端时由 adapter 覆写。
     *
     * <p>校验失败通过 {@link ModelValidation#isValid()} 表达，不抛异常。</p>
     */
    default ModelValidation validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "model id is required", 0L);
        }
        long start = System.currentTimeMillis();
        double[] vector;
        try {
            vector = embed("ping");
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "embedding probe failed" : e.getMessage();
            return ModelValidation.invalid(model, message, System.currentTimeMillis() - start);
        }
        long elapsed = System.currentTimeMillis() - start;
        if (vector == null || vector.length == 0) {
            return ModelValidation.invalid(model, "embedding probe returned no vector", elapsed);
        }
        return ModelValidation.valid(model, elapsed);
    }
}
