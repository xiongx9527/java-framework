package com.xiongx9527.fw.plus.ai.support.retrieval.graph;

import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/** 图节点（GraphRAG 预留）。 */
@Getter
@Builder
public class GraphNode {
    private final String id;
    private final String label;
    @Builder.Default
    private final Map<String, Object> metadata = Collections.emptyMap();
    private final float[] vector;
}
