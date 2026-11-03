package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * 检索请求：topK 为每路召回上限，topN 为最终返回上限，
 * scoreThreshold 作用在融合/重排后的最终分上（对齐 Dify）。
 * corpus 为词法检索的候选集合；向量检索忽略它（由向量库承担）。
 */
@Getter
@Builder
public class RetrievalQuery {
    private final String text;
    @Builder.Default
    private final int topK = 10;
    @Builder.Default
    private final int topN = 8;
    @Builder.Default
    private final double scoreThreshold = 0.0;
    @Builder.Default
    private final Map<String, Object> filters = Collections.emptyMap();
    @Builder.Default
    private final List<Document> corpus = Collections.emptyList();
}
