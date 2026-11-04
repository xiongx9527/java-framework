package com.xiongx9527.fw.plus.ai.base.store.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内存图存储参考实现（邻接表 + 节点表）。
 *
 * <p>{@code searchNodes} 按标签 / 元数据文本做包含匹配（大小写不敏感），最多 {@code topK} 个；
 * 向量字段不参与检索——它是留给真实后端（Neo4j 等）做 embedding 召回的，内存实现只做确定性文本匹配，
 * 便于单测与本地调试。</p>
 */
public class InMemoryGraphStore implements GraphStorePort {

    private final Map<String, GraphNode> nodesById = new LinkedHashMap<String, GraphNode>();

    private final Map<String, List<GraphEdge>> outbound = new LinkedHashMap<String, List<GraphEdge>>();

    public synchronized void addNode(GraphNode node) {
        if (node == null || node.getId() == null) {
            return;
        }
        nodesById.put(node.getId(), node);
    }

    public synchronized void addEdge(GraphEdge edge) {
        if (edge == null || edge.getFromId() == null) {
            return;
        }
        List<GraphEdge> list = outbound.get(edge.getFromId());
        if (list == null) {
            list = new ArrayList<GraphEdge>();
            outbound.put(edge.getFromId(), list);
        }
        list.add(edge);
    }

    @Override
    public boolean health() {
        return true;
    }

    @Override
    public synchronized List<GraphEdge> neighbors(String nodeId, Set<String> edgeTypes) {
        List<GraphEdge> edges = outbound.get(nodeId);
        if (edges == null || edges.isEmpty()) {
            return new ArrayList<GraphEdge>();
        }
        List<GraphEdge> result = new ArrayList<GraphEdge>();
        for (GraphEdge edge : edges) {
            if (edgeTypes == null || edgeTypes.isEmpty() || edgeTypes.contains(edge.getType())) {
                result.add(edge);
            }
        }
        return result;
    }

    @Override
    public synchronized List<GraphNode> nodes(Collection<String> ids) {
        List<GraphNode> result = new ArrayList<GraphNode>();
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            GraphNode node = nodesById.get(id);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    @Override
    public synchronized List<GraphNode> searchNodes(String text, int topK) {
        List<GraphNode> matched = new ArrayList<GraphNode>();
        String needle = text == null ? null : text.toLowerCase();
        for (GraphNode node : nodesById.values()) {
            if (needle == null || needle.isEmpty()) {
                matched.add(node);
            } else if (matches(node, needle)) {
                matched.add(node);
            }
        }
        int limit = topK <= 0 ? matched.size() : Math.min(topK, matched.size());
        return new ArrayList<GraphNode>(matched.subList(0, limit));
    }

    private boolean matches(GraphNode node, String needle) {
        if (node.getLabel() != null && node.getLabel().toLowerCase().contains(needle)) {
            return true;
        }
        if (node.getMetadata() != null) {
            for (Map.Entry<String, Object> entry : node.getMetadata().entrySet()) {
                String value = entry.getValue() == null ? null : String.valueOf(entry.getValue()).toLowerCase();
                if (value != null && value.contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }
}
