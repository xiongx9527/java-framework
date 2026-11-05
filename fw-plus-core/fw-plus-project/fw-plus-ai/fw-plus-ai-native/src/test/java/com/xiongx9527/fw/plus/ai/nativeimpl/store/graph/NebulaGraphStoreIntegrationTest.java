package com.xiongx9527.fw.plus.ai.nativeimpl.store.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.xiongx9527.fw.plus.ai.base.store.StoreQuery;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphEdge;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphNode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 真实 Nebula Graph（nebula-http-gateway）冒烟测试：探测 SHOW SPACES 可用才跑，否则跳过。
 * 通过 -Dnebula.url / NEBULA_URL、-Dnebula.graphd / NEBULA_GRAPHD、-Dnebula.graphdPort / NEBULA_GRAPHD_PORT 指定。
 * 需 storaged 已注册且 ONLINE（否则建 space 会报 Host not enough）。
 */
class NebulaGraphStoreIntegrationTest {
    private static String setting(String property, String env, String fallback) {
        String value = System.getProperty(property, System.getenv(env));
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static Map<String, Object> meta(String content, String lang) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("content", content);
        m.put("lang", lang);
        return m;
    }

    @Test
    void saveNodesEdgesAndSearch() {
        String url = setting("nebula.url", "NEBULA_URL", "http://127.0.0.1:18080");
        String graphd = setting("nebula.graphd", "NEBULA_GRAPHD", "graphd");
        int port = Integer.parseInt(setting("nebula.graphdPort", "NEBULA_GRAPHD_PORT", "9669"));
        NebulaGraphStore store = new NebulaGraphStore(url, null, null, "ai_graph", graphd, port, 5000, 30000);
        assumeTrue(store.health(), "nebula gateway not reachable at " + url + "; skipping live test");

        store.clear();
        store.save(StoreRecord.builder().id("a").type("Person").content("alpha")
                .metadata(meta("alpha", "en")).vector(new double[]{1, 0, 0, 0}).build());
        store.save(StoreRecord.builder().id("b").type("Person").content("beta")
                .metadata(meta("beta", "fr")).vector(new double[]{0, 1, 0, 0}).build());
        store.addEdge(GraphEdge.builder().fromId("a").toId("b").type("knows").weight(0.9).build());

        List<GraphNode> nodes = store.nodes(Arrays.asList("a", "b"));
        assertEquals(2, nodes.size());

        List<GraphEdge> edges = store.neighbors("a", null);
        assertEquals(1, edges.size());
        assertEquals("b", edges.get(0).getToId());
        assertEquals("knows", edges.get(0).getType());

        List<StoreRecord> found = store.search(StoreQuery.builder().text("alpha").topK(5).build());
        assertFalse(found.isEmpty());
        assertEquals("a", found.get(0).getId());

        assertTrue(store.count() >= 2L);

        store.deleteAll(Arrays.asList("a", "b"));
        store.clear();
    }
}
