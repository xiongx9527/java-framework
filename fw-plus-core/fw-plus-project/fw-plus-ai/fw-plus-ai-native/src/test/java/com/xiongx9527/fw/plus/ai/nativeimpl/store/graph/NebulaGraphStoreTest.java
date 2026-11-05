package com.xiongx9527.fw.plus.ai.nativeimpl.store.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.store.StoreQuery;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphEdge;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphNode;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.FakeHttpServer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NebulaGraphStoreTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private static List<String> cols(String... c) {
        return Arrays.asList(c);
    }

    private static String ngql(List<String> columns, List<List<Object>> rows) throws Exception {
        List<Map<String, Object>> tables = new ArrayList<Map<String, Object>>();
        for (List<Object> r : rows) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            for (int i = 0; i < columns.size(); i++) {
                row.put(columns.get(i), r.get(i));
            }
            tables.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("headers", columns);
        data.put("tables", tables);
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("code", 0);
        root.put("data", data);
        root.put("message", "");
        return MAPPER.writeValueAsString(root);
    }

    private static String ok() throws Exception {
        return ngql(Collections.<String>emptyList(), Collections.<List<Object>>emptyList());
    }

    private static FakeHttpServer server() throws Exception {
        return new FakeHttpServer(request -> {
            String b = request.body;
            try {
                if (request.path.contains("/api/db/connect")) {
                    Map<String, String> headers = new LinkedHashMap<String, String>();
                    headers.put("Set-Cookie", "common-nsid=test-nsid; Path=/; HttpOnly");
                    return FakeHttpServer.json(200,
                            FakeHttpServer.body("{'code':0,'data':'test-id','message':'ok'}"), headers);
                }
                if (b.contains("CREATE SPACE") || b.contains("CREATE TAG") || b.contains("CREATE EDGE")) {
                    return FakeHttpServer.json(200, ok());
                }
                if (b.contains("SHOW SPACES")) {
                    return FakeHttpServer.json(200, ngql(cols("Name"),
                            Collections.singletonList(row("ai_graph"))));
                }
                if (b.contains("id(m) AS toId")) {
                    return FakeHttpServer.json(200, ngql(cols("toId", "type", "weight"),
                            Collections.singletonList(row("b", "knows", 0.8))));
                }
                if (b.contains("UPSERT EDGE")) {
                    return FakeHttpServer.json(200, ngql(cols("e"),
                            Collections.singletonList(row("rel"))));
                }
                // MATCH 中顶点属性必须带 tag 限定（n.Entity.prop），否则 Nebula 会返回 NULL
                if (b.contains("RETURN id(n) AS id, n.Entity.label AS label")) {
                    return FakeHttpServer.json(200, ngql(
                            cols("id", "label", "content", "meta", "vector"),
                            Collections.singletonList(row("a", "Person", "hello",
                                    MAPPER.writeValueAsString(Collections.singletonMap("x", 1)),
                                    "[0.1,0.2]"))));
                }
                if (b.contains("count(n) AS c")) {
                    return FakeHttpServer.json(200, ngql(cols("c"),
                            Collections.singletonList(row(7L))));
                }
                return FakeHttpServer.json(200, ngql(cols("id"),
                        Collections.singletonList(row("a"))));
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
        FakeHttpServer server = server();
        try {
            NebulaGraphStore store = new NebulaGraphStore(server.url(), "root", "nebula", "ai_graph");
            assertTrue(store.health());
            store.save(record());
            FakeHttpServer.Request save = server.lastRequest();
            assertTrue(save.body.contains("UPSERT VERTEX"));
            assertTrue(save.body.contains("Entity"));
            assertTrue(save.body.contains("meta"));

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
    void nodesAndNeighbors() throws Exception {
        FakeHttpServer server = server();
        try {
            NebulaGraphStore store = new NebulaGraphStore(server.url());
            List<GraphNode> nodes = store.nodes(Arrays.asList("a", "missing"));
            assertEquals(1, nodes.size());
            assertEquals("a", nodes.get(0).getId());
            assertEquals(0.1, nodes.get(0).getVector()[0], 1e-9);

            List<GraphEdge> edges = store.neighbors("a", null);
            assertEquals(1, edges.size());
            assertEquals("a", edges.get(0).getFromId());
            assertEquals("b", edges.get(0).getToId());
            assertEquals("knows", edges.get(0).getType());
            assertEquals(0.8, edges.get(0).getWeight(), 1e-9);
            assertTrue(server.lastRequest().body.contains("id(n) == "));
        } finally {
            server.close();
        }
    }

    @Test
    void searchAndCount() throws Exception {
        FakeHttpServer server = server();
        try {
            NebulaGraphStore store = new NebulaGraphStore(server.url());
            List<StoreRecord> results = store.search(StoreQuery.builder().text("hello").topK(5).build());
            assertEquals(1, results.size());
            assertEquals("a", results.get(0).getId());
            assertTrue(server.lastRequest().body.contains("CONTAINS"));
            assertEquals(7L, store.count());
        } finally {
            server.close();
        }
    }
}
