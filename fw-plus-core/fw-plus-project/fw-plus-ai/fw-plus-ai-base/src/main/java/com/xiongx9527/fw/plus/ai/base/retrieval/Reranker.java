package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import java.util.List;

/**
 * 重排：对候选重新排序。基础契约在 base，具体实现（模型重排等）在 support。
 */
public interface Reranker {

    List<ScoredDocument> rerank(String query, List<ScoredDocument> candidates);
}
