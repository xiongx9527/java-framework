package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import java.util.List;

/**
 * 检索器：返回按相关度降序的候选（分数 0~1）。
 */
public interface Retriever {

    List<ScoredDocument> retrieve(RetrievalQuery query);
}
