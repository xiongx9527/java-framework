package com.xiongx9527.fw.plus.ai.nativeimpl.store.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.store.StoreQuery;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphEdge;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphNode;
import com.xiongx9527.fw.plus.ai.nativeimpl.support.FakeHttpServer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Neo4jGraphStoreTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TX = "/db/neo4j/tx/commit";

    private static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    @SuppressWarnings("unchecked")
    private static String tx(String[] columns, List<List<Object>> rows, String cypherError) throws Exception {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("columns", Arrays.asList(columns));
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        for (List<Object> r : rows) {
            Map<String, Object> d = new LinkedHashMap<String, Object>();
            d.put("row", r);
            data.add(d);
        }
        result.put("data", data);
        List<Map<String, Object>> errors = new ArrayList<Map<String, Object>>();
        if (cypherError != null) {
            Map<String, Object> e = new LinkedHashMap<String, Object>();
            e.put("message", cypherError);
            errors.add(e);
        }
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("results", Collections.singletonList(result));
        root.put("errors", errors);
        return MAPPER.writeValueAsString(root);
    }

    private static FakeHttpServer server(final boolean emptyUpdate, final String cypherError) throws Exception {
        final String metaJson = MAPPER.writeValueAsString(Collections.singletonMap("x", 1));
        return new FakeHttpServer(request -> {
            if (!"POST".equals(request.method)) {
                return FakeHttpServer.json(200, "{}");
            }
            String b = request.body;
            try {
                if (cypherError != null) {
                    return FakeHttpServer.json(200, tx(new String[]{"id"}, Collections.<List<Object>>emptyList(), cypherError));
                }
                if (b.contains("DETACH DELETE n RETURN count(n) AS deleted")) {
                    return FakeHttpServer.json(200, tx(new String[]{"deleted"},
                            Collections.singletonList(row(2)), null));
                }
                if (b.contains("RETURN count(n) AS c")) {
                    return FakeHttpServer.json(200, tx(new String[]{"c"},
                            Collections.singletonList(row(7)), null));
                }
                if (b.contains("RETURN m.id AS toId")) {
                    return FakeHttpServer.json(200, tx(new String[]{"toId", "type", "weight"},
                            Collections.singletonList(row("b", "knows", 0.8)), null));
                }
                if (b.contains("MERGE (a)-[r:__EDGE")) {
                    return FakeHttpServer.json(200, tx(new String[]{"r"},
                            Collections.singletonList(row("rel")), null));
                }
                if (b.contains("MATCH (n:__Node {id:$id}) SET")) {
                    return FakeHttpServer.json(200, tx(new String[]{"id"},
                            emptyUpdate ? Collections.<List<Object>>emptyList()
                                    : Collections.singletonList(row("a")), null));
                }
                if (b.contains("MERGE (n:__Node")) {
                    return FakeHttpServer.json(200, tx(new String[]{"id"},
                            Collections.singletonList(row("a")), null));
                }
                if (b.contains("RETURN n.id AS id, n.label AS label")) {
                    return FakeHttpServer.json(200, tx(
                            new String[]{"id", "label", "content", "meta", "vector"},
                            Collections.singletonList(row("a", "Person", "hello", metaJson,
                                    Arrays.asList(0.1, 0.2))), null));
                }
                return FakeHttpServer.json(200, tx(new String[]{"id"},
                        Collections.singletonList(row("a")), null));
            } catch (Exception ex) {
                return FakeHttpServer.json(500, FakeHttpServer.body("{'error':'handler failed'}"));
            }
        });
    }

    private static StoreRecord record() {
        return StoreRecord.builder().id("a").type("Person").content("hello")
                .metadata(Collections.<String, Object>singletonMap("x", 1))
                .vector(new double[]{0.1, 0.2}).build();
    }

    @Test
    void healthSaveAndGetRoundTrip() throws Exception {
        FakeHttpServer server = server(false, null);
        try {
            Neo4jGraphStore store = new Neo4jGraphStore(server.url(), "neo4j", "pwd");
            assertTrue(store.health());
            store.save(record());
            FakeHttpServer.Request save = server.lastRequest();
            assertEquals(TX, save.path);
            assertTrue(save.header("Authorization").startsWith("Basic "));
            assertTrue(save.body.contains("MERGE (n:__Node"));
            assertTrue(save.body.contains("__meta"));
            assertTrue(save.body.contains("vector"));

            StoreRecord loaded = store.get("a");
            assertNotNull(loaded);
            assertEquals("a", loaded.getId());
            assertEquals("Person", loaded.getType());
            assertEquals("hello", loaded.getContent());
            assertEquals(1, loaded.getMetadata().get("x"));
            assertEquals(2, loaded.getVector().length);
        } finally {
            server.close();
        }
    }

    @Test
    void nodesSearchNodesAndStoreQuery() throws Exception {
        FakeHttpServer server = server(false, null);
        try {
            Neo4jGraphStore store = new Neo4jGraphStore(server.url());
            List<GraphNode> nodes = store.nodes(Arrays.asList("a", "missing"));
            assertEquals(1, nodes.size());
            assertEquals("a", nodes.get(0).getId());
            assertEquals("Person", nodes.get(0).getLabel());
            assertEquals(0.1, nodes.get(0).getVector()[0], 1e-9);

            List<GraphNode> found = store.searchNodes("hello", 5);
            assertEquals(1, found.size());

            List<StoreRecord> results = store.search(StoreQuery.builder().text("hello").topK(5).build());
            assertEquals(1, results.size());
            assertEquals("a", results.get(0).getId());
            assertTrue(server.lastRequest().body.contains("CONTAINS $text"));

            List<StoreRecord> filtered = store.search(StoreQuery.builder().text("hello").topK(5)
                    .filter(Collections.<String, Object>singletonMap("x", 1)).build());
            assertEquals(1, filtered.size());
            List<StoreRecord> rejected = store.search(StoreQuery.builder().text("hello").topK(5)
                    .filter(Collections.<String, Object>singletonMap("x", 99)).build());
            assertTrue(rejected.isEmpty());
        } finally {
            server.close();
        }
    }

    @Test
    void addEdgeAndNeighbors() throws Exception {
        FakeHttpServer server = server(false, null);
        try {
            Neo4jGraphStore store = new Neo4jGraphStore(server.url());
            store.addNode(GraphNode.builder().id("a").label("Person").build());
            store.addEdge(GraphEdge.builder().fromId("a").toId("b").type("knows").weight(0.8).build());
            assertTrue(server.lastRequest().body.contains("MERGE (a)-[r:__EDGE"));
            assertTrue(server.lastRequest().body.contains("r.weight=$weight"));

            List<GraphEdge> edges = store.neighbors("a", null);
            assertEquals(1, edges.size());
            assertEquals("a", edges.get(0).getFromId());
            assertEquals("b", edges.get(0).getToId());
            assertEquals("knows", edges.get(0).getType());
            assertEquals(0.8, edges.get(0).getWeight(), 1e-9);
            assertTrue(server.lastRequest().body.contains("size($types)=0 OR r.type IN $types"));
        } finally {
            server.close();
        }
    }

    @Test
    void deleteClearAndCount() throws Exception {
        FakeHttpServer server = server(false, null);
        try {
            Neo4jGraphStore store = new Neo4jGraphStore(server.url());
            assertTrue(store.delete("a"));
            assertTrue(server.lastRequest().body.contains("DETACH DELETE n"));
            store.clear();
            assertEquals(7L, store.count());
        } finally {
            server.close();
        }
    }

    @Test
    void updateMissingNodeThrows() throws Exception {
        FakeHttpServer server = server(true, null);
        try {
            Neo4jGraphStore store = new Neo4jGraphStore(server.url());
            assertThrows(IllegalStateException.class, () -> store.update(record()));
        } finally {
            server.close();
        }
    }

    @Test
    void cypherErrorPropagates() throws Exception {
        FakeHttpServer server = server(false, "syntax error");
        try {
            Neo4jGraphStore store = new Neo4jGraphStore(server.url());
            assertThrows(IllegalStateException.class, () -> store.saveAll(Collections.singletonList(record())));
        } finally {
            server.close();
        }
    }
}
