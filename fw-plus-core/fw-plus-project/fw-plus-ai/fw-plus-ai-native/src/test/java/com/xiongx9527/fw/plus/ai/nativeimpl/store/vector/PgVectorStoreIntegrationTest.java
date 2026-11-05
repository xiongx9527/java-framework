package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 真实 PostgreSQL + pgvector 冒烟测试：探测连接可用才跑，否则跳过。
 * 通过 -Dpgvector.url / PGVECTOR_URL、-Dpgvector.user / PGVECTOR_USER、-Dpgvector.password / PGVECTOR_PASSWORD 指定。
 * 依赖目标库已可 CREATE EXTENSION vector（或已安装 pgvector）。
 */
class PgVectorStoreIntegrationTest {
    private static final String COL = "fw_native_test";

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
    void upsertAndSearch() {
        String url = setting("pgvector.url", "PGVECTOR_URL", "jdbc:postgresql://127.0.0.1:5432/postgres");
        String user = setting("pgvector.user", "PGVECTOR_USER", null);
        String password = setting("pgvector.password", "PGVECTOR_PASSWORD", null);
        PgVectorStore store = new PgVectorStore(url, user, password, COL);
        assumeTrue(store.health(), "postgres not reachable at " + url + "; skipping live test");

        store.dropCollection(COL);
        store.ensureCollection(COL, 4);
        assertEquals(Integer.valueOf(4), store.collectionDimension(COL));
        store.upsert(COL, "a", new double[]{1, 0, 0, 0}, meta("alpha", "en"));
        store.upsert(COL, "b", new double[]{0, 1, 0, 0}, meta("beta", "fr"));

        VectorSearchResult all = store.search(VectorSearchRequest.builder()
                .collection(COL).vector(new double[]{1, 0, 0, 0}).topK(2).build());
        assertFalse(all.isEmpty());
        assertEquals("a", all.getHits().get(0).getDocument().getId());
        assertEquals("alpha", all.getHits().get(0).getDocument().getMetadata().get("content"));

        VectorSearchResult byCandidate = store.search(VectorSearchRequest.builder()
                .collection(COL).vector(new double[]{1, 0, 0, 0}).topK(2)
                .candidateIds(Arrays.asList("b")).build());
        assertEquals(1, byCandidate.size());
        assertEquals("b", byCandidate.getHits().get(0).getDocument().getId());

        VectorSearchResult byFilter = store.search(VectorSearchRequest.builder()
                .collection(COL).vector(new double[]{1, 0, 0, 0}).topK(2)
                .filter("lang", "fr").build());
        assertEquals(1, byFilter.size());
        assertEquals("b", byFilter.getHits().get(0).getDocument().getId());

        assertThrows(VectorStorePort.DimensionMismatchException.class,
                () -> store.ensureCollection(COL, 8));

        store.delete(COL, "a");
        store.dropCollection(COL);
    }
}
