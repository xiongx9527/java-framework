package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;

/**
 * 模型级监听：与按请求的 {@link LlmListener} 不同，它在「模型实例」粒度上观测，
 * 不绑定某一次请求——用于健康度、成本、调用量等跨请求统计。
 *
 * <p>配合 {@link ListeningLlmPort} 使用：构造时注入，每次调用前后统一派发，业务无需逐请求挂监听。</p>
 */
public interface ChatModelListener extends AiListener {

    /** 模型调用开始（按 modelId 聚合）。 */
    default void onModelRequest(String modelId) {
    }

    /** 模型调用成功（带耗时）。 */
    default void onModelResponse(String modelId, long latencyMillis) {
    }

    /** 模型调用失败。 */
    default void onModelError(String modelId, String message) {
    }
}
