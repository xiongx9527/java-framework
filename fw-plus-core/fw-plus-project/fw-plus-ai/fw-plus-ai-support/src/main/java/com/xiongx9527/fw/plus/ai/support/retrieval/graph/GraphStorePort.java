package com.xiongx9527.fw.plus.ai.support.retrieval.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 图存储端口（GraphRAG 预留，未实现）。
 *
 * <p>由 adapter 用 Neo4j / Postgres 等实现；多跳扩展、PageRank、实体链接策略留待真正实现时再定。</p>
 */
public interface GraphStorePort {

    List<GraphEdge> neighbors(String nodeId, Set<String> edgeTypes);

    List<GraphNode> nodes(Collection<String> ids);

    List<GraphNode> searchNodes(String text, int topK);
}
