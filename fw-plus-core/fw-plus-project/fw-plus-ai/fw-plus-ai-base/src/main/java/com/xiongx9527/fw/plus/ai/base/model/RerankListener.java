package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;
import java.util.List;

/**
 * 重排模型调用事件：请求 → 响应 / 失败。
 */
public interface RerankListener extends AiListener {

    /** 重排开始。 */
    default void onRequest(String query, int candidateCount) {
    }

    /** 重排完成；{@code scores} 与候选一一对齐。 */
    default void onResponse(List<Double> scores) {
    }
}
