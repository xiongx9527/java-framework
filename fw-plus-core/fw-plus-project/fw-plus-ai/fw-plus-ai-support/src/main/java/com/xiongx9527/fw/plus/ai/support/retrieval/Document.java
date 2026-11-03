package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/** 通用检索文档：id + 内容 + 元数据 + 可选向量。 */
@Getter
@Builder
public class Document {
    private final String id;
    private final String content;
    @Builder.Default
    private final Map<String, Object> metadata = Collections.emptyMap();
    private final float[] vector;
}
