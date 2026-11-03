package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;
import java.util.List;

/**
 * 向量模型调用事件：请求 → 响应 / 失败。
 */
public interface EmbeddingListener extends AiListener {

    /** 编码开始。 */
    default void onRequest(List<String> texts) {
    }

    /** 编码完成；{@code count} 为成功条数，{@code dimension} 为向量维度（未知为 0）。 */
    default void onResponse(int count, int dimension) {
    }
}
