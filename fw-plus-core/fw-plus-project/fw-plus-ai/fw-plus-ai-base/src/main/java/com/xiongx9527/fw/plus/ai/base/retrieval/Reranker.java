package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.List;

/**
 * 重排：对候选重新打分并排序。条目类型与检索器一致。
 */
public interface Reranker<T> {

    List<Retrieved<T>> rerank(String query, List<Retrieved<T>> candidates);

    /**
     * 用 {@link RerankPort} 构造文档重排器，省去业务侧自行拼装。
     *
     * <p>放在 base 而不是各个 adapter：任何后端产出的 {@code RerankPort} 都能一步接到
     * 检索链上，无需引入增强层依赖，也不必在每个 adapter 里重复这套组合逻辑。</p>
     */
    static Reranker<Document> from(RerankPort port) {
        return new DefaultReranker(port);
    }
}
