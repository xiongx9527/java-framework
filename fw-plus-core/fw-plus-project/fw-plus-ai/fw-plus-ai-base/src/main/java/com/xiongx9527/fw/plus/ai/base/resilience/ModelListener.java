package com.xiongx9527.fw.plus.ai.base.resilience;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;

/**
 * 模型边界监听：按模型 id 记录请求 / 响应 / 失败，用于健康度与成本统计。
 *
 * <p>与 base 的 {@code LlmListener} 的分工：LlmListener 面向「一次调用的事件」，
 * 本接口面向「按模型聚合的观测」，由 adapter 挂在调用边界上。</p>
 */
public interface ModelListener extends AiListener {

    default void onRequest(String modelId) {
    }

    default void onResponse(String modelId, long latencyMillis) {
    }

    default void onFailure(String modelId, String message) {
    }
}
