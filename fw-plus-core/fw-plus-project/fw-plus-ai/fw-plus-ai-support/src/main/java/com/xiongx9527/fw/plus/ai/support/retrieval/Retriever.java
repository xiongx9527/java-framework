package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.List;

/** 检索器：返回按相关度排序的候选（分数 0~1）。 */
public interface Retriever {
    List<ScoredDocument> retrieve(RetrievalQuery query);
}
