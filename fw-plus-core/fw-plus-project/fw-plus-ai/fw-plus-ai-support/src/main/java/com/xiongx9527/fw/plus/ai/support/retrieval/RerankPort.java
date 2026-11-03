package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.List;

/** 重排模型端口：返回与 documents 下标对齐的分数，由 product 注入。 */
public interface RerankPort {
    List<Double> score(String query, List<String> documents);
}
