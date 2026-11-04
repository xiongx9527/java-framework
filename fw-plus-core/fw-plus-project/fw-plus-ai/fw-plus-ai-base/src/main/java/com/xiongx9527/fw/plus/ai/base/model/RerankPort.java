package com.xiongx9527.fw.plus.ai.base.model;

import java.util.Collections;
import java.util.List;

/**
 * 重排模型端口：对候选文本按与 query 的相关度重新打分。
 *
 * <p>返回列表与 {@code documents} 下标一一对齐，分数取值 0~1。实现应保证长度一致；
 * 长度不足时调用方按「沿用原分」处理。</p>
 */
public interface RerankPort {

    List<Double> score(String query, List<String> documents);

    /**
     * Model-bearing score. Default ignores the model id and delegates to {@link #score(String, List)};
     * adapters that can address multiple rerank models override this to switch models (e.g. failover).
     *
     * @param model  rerank model id; null uses the adapter default
     */
    default List<Double> score(String model, String query, List<String> documents) {
        return score(query, documents);
    }

    /**
     * 枚举该后端可用的重排模型，供配置页选型使用。
     *
     * <p>不支持枚举的后端返回空列表（不抛异常）；列表项的 {@link ModelInfo#getType()} 为
     * {@link ModelType#RERANK}。</p>
     */
    default List<ModelInfo> listModels() {
        return ModelCatalogSupport.empty();
    }

    /**
     * 校验给定模型是否可用：默认用当前实例真实打一次分，返回结果非空即视为可用
     * （地址、鉴权、模型名同时被验证）。需要按模型名切换后端时由 adapter 覆写。
     *
     * <p>校验失败通过 {@link ModelValidation#isValid()} 表达，不抛异常。</p>
     */
    default ModelValidation validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "model id is required", 0L);
        }
        long start = System.currentTimeMillis();
        List<Double> scores;
        try {
            scores = score("ping", Collections.singletonList("ping"));
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "rerank probe failed" : e.getMessage();
            return ModelValidation.invalid(model, message, System.currentTimeMillis() - start);
        }
        long elapsed = System.currentTimeMillis() - start;
        if (scores == null || scores.isEmpty()) {
            return ModelValidation.invalid(model, "rerank probe returned no score", elapsed);
        }
        return ModelValidation.valid(model, elapsed);
    }
}
