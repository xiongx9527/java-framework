package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;
import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import java.util.List;

/**
 * 检索事件：查询 → 命中 / 失败。
 */
public interface RetrievalListener extends AiListener {

    /** 检索开始（重排与阈值过滤之前）。 */
    default void onQuery(RetrievalQuery query) {
    }

    /** 检索完成，{@code hits} 为最终结果（已重排、已阈值过滤、已 topN 截断）。 */
    default void onHits(List<ScoredDocument> hits) {
    }
}
