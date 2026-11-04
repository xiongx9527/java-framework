package com.xiongx9527.fw.plus.ai.base.store.graph;

import com.xiongx9527.fw.plus.ai.base.store.Store;
import com.xiongx9527.fw.plus.ai.base.store.StoreQuery;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内存图存储参考实现（邻接表 + 节点表），同时实现 {@link Store} 公共契约。
 */
public class InMemoryGraphStore implements GraphStorePort {

    private final String name;

    private final Map<String, GraphNode> nodesById = new LinkedHashMap<String, GraphNode>();

    private final Map<String, List<GraphEdge>> outbound = new LinkedHashMap<String, List<GraphEdge>>();

    public InMemoryGraphStore() {
        this("graph");
    }

    public InMemoryGraphStore(String name) {
        this.name = name == null ? "graph" : name;
    }

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

    @Override
    public String name() {
        return name;
    }

    @Override
    public StoreType type() {
        return StoreType.GRAPH;
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public boolean health() {
        return true;
    }

    @Override
    public synchronized void save(StoreRecord record) {
        if (record == null || record.getId() == null) {
            return;
        }
        addNode(GraphNode.builder().id(record.getId()).label(record.getType())
                .metadata(record.getMetadata()).vector(record.getVector()).build());
    }

    @Override
    public synchronized void saveAll(Collection<StoreRecord> records) {
        if (records != null) {
            for (StoreRecord r : records) {
                save(r);
            }
        }
    }

    @Override
    public synchronized void update(StoreRecord record) {
        if (record == null || record.getId() == null) {
            throw new IllegalArgumentException("record id is required");
        }
        if (!nodesById.containsKey(record.getId())) {
            throw new IllegalStateException("node does not exist: " + record.getId());
        }
        addNode(GraphNode.builder().id(record.getId()).label(record.getType())
                .metadata(record.getMetadata()).vector(record.getVector()).build());
    }

    @Override
    public synchronized long deleteAll(Collection<String> ids) {
        long removed = 0L;
        if (ids != null) {
            for (String id : ids) {
                if (delete(id)) {
                    removed++;
                }
            }
        }
        return removed;
    }

    @Override
    public synchronized StoreRecord get(String id) {
        GraphNode node = nodesById.get(id);
        return node == null ? null : toRecord(node);
    }

    @Override
    public synchronized List<StoreRecord> getAll(Collection<String> ids) {
        List<StoreRecord> out = new ArrayList<StoreRecord>();
        if (ids != null) {
            for (String id : ids) {
                StoreRecord r = get(id);
                if (r != null) {
                    out.add(r);
                }
            }
        }
        return out;
    }

    @Override
    public synchronized boolean delete(String id) {
        if (id == null) {
            return false;
        }
        boolean existed = nodesById.remove(id) != null;
        outbound.remove(id);
        for (List<GraphEdge> list : outbound.values()) {
            list.removeIf(edge -> id.equals(edge.getToId()));
        }
        return existed;
    }

    @Override
    public synchronized List<StoreRecord> search(StoreQuery query) {
        if (query == null) {
            return new ArrayList<StoreRecord>();
        }
        String text = query.getText();
        Map<String, Object> filter = query.getFilter();
        int topK = query.getTopK();
        String needle = text == null ? null : text.toLowerCase();
        List<StoreRecord> out = new ArrayList<StoreRecord>();
        for (GraphNode node : nodesById.values()) {
            if (needle != null && !needle.isEmpty() && !matches(node, needle)) {
                continue;
            }
            if (filter != null && !filter.isEmpty() && !matchesFilter(node, filter)) {
                continue;
            }
            out.add(toRecord(node));
            if (topK > 0 && out.size() >= topK) {
                break;
            }
        }
        return out;
    }

    @Override
    public synchronized void clear() {
        nodesById.clear();
        outbound.clear();
    }

    @Override
    public synchronized long count() {
        return nodesById.size();
    }

    private StoreRecord toRecord(GraphNode node) {
        return StoreRecord.builder().id(node.getId()).type(node.getLabel())
                .metadata(node.getMetadata()).vector(node.getVector()).build();
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

    private boolean matchesFilter(GraphNode node, Map<String, Object> filter) {
        Map<String, Object> meta = node.getMetadata();
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            Object actual = meta == null ? null : meta.get(entry.getKey());
            if (!filterEquals(actual, entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean filterEquals(Object actual, Object expected) {
        if (actual == null && expected == null) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        if (actual instanceof Number && expected instanceof Number) {
            return ((Number) actual).doubleValue() == ((Number) expected).doubleValue();
        }
        return actual.equals(expected);
    }
}
