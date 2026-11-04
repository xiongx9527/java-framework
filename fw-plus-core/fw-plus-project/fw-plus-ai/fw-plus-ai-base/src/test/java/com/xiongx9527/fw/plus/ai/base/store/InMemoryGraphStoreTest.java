package com.xiongx9527.fw.plus.ai.base.store;

import com.xiongx9527.fw.plus.ai.base.store.graph.GraphEdge;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphNode;
import com.xiongx9527.fw.plus.ai.base.store.graph.InMemoryGraphStore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class InMemoryGraphStoreTest {

    @Test
    public void neighborsFiltersByEdgeType() {
        InMemoryGraphStore store = new InMemoryGraphStore();
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
        InMemoryGraphStore store = new InMemoryGraphStore();
        store.addNode(GraphNode.builder().id("a").label("X").build());
        store.addNode(GraphNode.builder().id("b").label("Y").build());
        List<GraphNode> found = store.nodes(Arrays.asList("a", "missing", "b"));
        assertEquals(2, found.size());
    }

    @Test
    public void searchNodesMatchesLabelAndMetadata() {
        InMemoryGraphStore store = new InMemoryGraphStore();
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
        InMemoryGraphStore store = new InMemoryGraphStore();
        store.addNode(GraphNode.builder().id("a").label("Person").build());
        store.addNode(GraphNode.builder().id("b").label("Person").build());
        List<GraphNode> hits = store.searchNodes("Person", 1);
        assertEquals(1, hits.size());
    }

    @Test
    public void healthIsTrue() {
        org.junit.Assert.assertTrue(new InMemoryGraphStore().health());
    }
}
