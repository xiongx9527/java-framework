package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.List;

/** 重排扩展：融合后对候选重新排序。 */
public interface Reranker {
    List<ScoredDocument> rerank(String query, List<ScoredDocument> candidates);
}
