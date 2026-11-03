package com.xiongx9527.fw.plus.ai.base.resilience;

/**
 * 模型健康度：记录调用结果并判断可用性（熔断 / 降权由实现决定）。
 */
public interface ModelHealthPort {

    /** 调用结果。 */
    enum Outcome {
        SUCCESS,
        FAILURE
    }

    void record(String modelId, Outcome outcome);

    /** 当前是否可用；未记录过的模型视为可用。 */
    default boolean available(String modelId) {
        return true;
    }
}
