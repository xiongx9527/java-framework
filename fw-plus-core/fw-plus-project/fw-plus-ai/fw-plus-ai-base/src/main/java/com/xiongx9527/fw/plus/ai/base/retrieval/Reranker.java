package com.xiongx9527.fw.plus.ai.base.retrieval;

import java.util.List;

/**
 * 重排：对候选重新打分并排序。条目类型与检索器一致。
 */
public interface Reranker<T> {

    List<Retrieved<T>> rerank(String query, List<Retrieved<T>> candidates);
}
