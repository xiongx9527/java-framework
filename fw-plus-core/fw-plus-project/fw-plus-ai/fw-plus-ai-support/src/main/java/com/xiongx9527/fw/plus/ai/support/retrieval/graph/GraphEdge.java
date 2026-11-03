package com.xiongx9527.fw.plus.ai.support.retrieval.graph;

import lombok.Builder;
import lombok.Getter;

/** 图边（GraphRAG 预留）。 */
@Getter
@Builder
public class GraphEdge {
    private final String fromId;
    private final String toId;
    private final String type;
    @Builder.Default
    private final double weight = 1.0;
}
