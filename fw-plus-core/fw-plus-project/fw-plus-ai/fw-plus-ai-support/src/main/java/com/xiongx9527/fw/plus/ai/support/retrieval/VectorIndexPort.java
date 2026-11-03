package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.List;
import java.util.Map;

/** 向量索引端口：由 product 用其向量库（Qdrant 等）实现。 */
public interface VectorIndexPort {
    List<ScoredDocument> search(float[] vector, int topK, Map<String, Object> filters);
}
