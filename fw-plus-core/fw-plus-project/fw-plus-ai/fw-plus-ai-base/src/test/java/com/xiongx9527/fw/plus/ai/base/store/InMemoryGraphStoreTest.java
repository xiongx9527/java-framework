package com.xiongx9527.fw.plus.ai.base.store;

import com.xiongx9527.fw.plus.ai.base.store.graph.GraphEdge;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphNode;
import com.xiongx9527.fw.plus.ai.base.store.graph.InMemoryGraphStore;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryGraphStoreTest {

    @Test
    public void neighborsFiltersByEdgeType() {
        InMemoryGraphStore store = new InMemoryGraphStore("g");
        store.addNode(GraphNode.builder().id("a").label("Person").build());
        store.addNode(GraphNode.builder().id("b").label("Person").build());
        store.addEdge(GraphEdge.builder().fromId("a").toId("b").type("knows").build());
        store.addEdge(GraphEdge.builder().fromId("a").toId("b").type("works").build());

        List<GraphEdge> all = store.neighbors("a", null);
        assertEquals(2, all.size());
        List<GraphEdge> knows = store.neighbors("a", java.util.Collections.singleton("knows"));
        assertEquals(1, knows.size());
        assertEquals("knows", knows.get(0).getType());
    }

    @Test
    public void nodesBatchesAndSkipsMissing() {
        InMemoryGraphStore store = new InMemoryGraphStore("g");
        store.addNode(GraphNode.builder().id("a").label("X").build());
        store.addNode(GraphNode.builder().id("b").label("Y").build());
        List<GraphNode> found = store.nodes(Arrays.asList("a", "missing", "b"));
        assertEquals(2, found.size());
    }

    @Test
    public void searchNodesMatchesLabelAndMetadata() {
        InMemoryGraphStore store = new InMemoryGraphStore("g");
        store.addNode(GraphNode.builder().id("a").label("Person").metadata(
                java.util.Collections.singletonMap("name", "Alice")).build());
        store.addNode(GraphNode.builder().id("b").label("Company").build());

        List<GraphNode> byLabel = store.searchNodes("Company", 10);
        assertEquals(1, byLabel.size());
        List<GraphNode> byMeta = store.searchNodes("alice", 10);
        assertEquals(1, byMeta.size());
        assertEquals("a", byMeta.get(0).getId());
    }

    @Test
    public void searchNodesRespectsTopK() {
        InMemoryGraphStore store = new InMemoryGraphStore("g");
        store.addNode(GraphNode.builder().id("a").label("Person").build());
        store.addNode(GraphNode.builder().id("b").label("Person").build());
        List<GraphNode> hits = store.searchNodes("Person", 1);
        assertEquals(1, hits.size());
    }

    @Test
    public void healthAndIdentity() {
        InMemoryGraphStore store = new InMemoryGraphStore("mine");
        assertTrue(store.health());
        assertEquals("mine", store.name());
        assertEquals(com.xiongx9527.fw.plus.ai.base.store.StoreType.GRAPH, store.type());
        assertTrue(store.configured());
    }

    @Test
    public void storeCrudSaveGetDeleteClearCount() {
        InMemoryGraphStore store = new InMemoryGraphStore("g");
        store.save(com.xiongx9527.fw.plus.ai.base.store.StoreRecord.builder()
                .id("n1").type("Person").metadata(java.util.Collections.singletonMap("k", "v")).build());
        assertEquals(1L, store.count());

        com.xiongx9527.fw.plus.ai.base.store.StoreRecord got = store.get("n1");
        assertEquals("n1", got.getId());
        assertEquals("Person", got.getType());

        List<com.xiongx9527.fw.plus.ai.base.store.StoreRecord> found =
                store.search(com.xiongx9527.fw.plus.ai.base.store.StoreQuery.builder().text("Person").topK(10).build());
        assertEquals(1, found.size());

        assertTrue(store.delete("n1"));
        assertFalse(store.delete("n1"));
        assertNull(store.get("n1"));
        assertEquals(0L, store.count());

        store.save(com.xiongx9527.fw.plus.ai.base.store.StoreRecord.builder().id("n2").type("X").build());
        store.clear();
        assertEquals(0L, store.count());
    }
}
