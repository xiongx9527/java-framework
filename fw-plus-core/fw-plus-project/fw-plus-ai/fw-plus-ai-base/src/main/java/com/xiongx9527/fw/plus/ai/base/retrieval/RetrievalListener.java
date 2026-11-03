package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;
import java.util.List;

/**
 * 检索事件：查询 → 命中 / 失败。
 */
public interface RetrievalListener extends AiListener {

    /** 检索开始。 */
    default void onQuery(RetrievalQuery query) {
    }

    /** 检索完成；{@code hits} 为当前阶段结果（是否已重排 / 截断取决于装饰器顺序）。 */
    default void onHits(List<? extends Retrieved<?>> hits) {
    }

    /** 检索上下文被注入对话时的回调（内容为注入段落，可能为 {@code null}）。 */
    default void onContext(String content) {
    }
}
