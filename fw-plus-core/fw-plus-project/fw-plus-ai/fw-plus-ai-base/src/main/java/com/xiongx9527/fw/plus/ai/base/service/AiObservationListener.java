package com.xiongx9527.fw.plus.ai.base.service;

/**
 * AI 服务调用的观测监听：一次调用开始 / 结束 / 出错时回调，携带 {@link AiCallObservation} 快照。
 *
 * <p>把「意图分类 → 检索增强 → 工具循环 → 最终回答」收敛成一条调用级 trace，
 * 便于上报到观测平台。Langfuse / Langsmith 等具体后端在 {@code support} 层实现本接口
 * （亦可同时实现 {@code LlmListener} 接收更细粒度的模型事件）。</p>
 */
public interface AiObservationListener {

    /** 调用即将执行（已拿到用户输入、尚未调模型）。 */
    default void onStart(AiCallObservation observation) {
    }

    /** 调用成功结束（已拿到最终回答 / latency）。 */
    default void onEnd(AiCallObservation observation) {
    }

    /** 调用抛错（含审核拒绝、模型异常、JSON 提取失败等）。 */
    default void onError(AiCallObservation observation) {
    }
}
