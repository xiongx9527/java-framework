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
 * 真实 Elasticsearch 8.x 冒烟测试：探测 /_cluster/health 可用才跑，否则跳过。
 * 通过 -Des.url / ES_URL、-Des.user / ES_USER、-Des.password / ES_PASSWORD 指定。
 * 需 ES 8+（顶层 knn 检索）；ES 7.x 请勿使用。
 */
class ElasticsearchVectorStoreIntegrationTest {
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
    void upsertAndSearch() throws Exception {
        String url = setting("es.url", "ES_URL", "http://localhost:9201");
        String user = setting("es.user", "ES_USER", null);
        String password = setting("es.password", "ES_PASSWORD", null);
        ElasticsearchVectorStore store = new ElasticsearchVectorStore(url, null, user, password, COL, 5000, 10000);
        assumeTrue(store.health(), "elasticsearch not reachable/authorized at " + url + "; skipping live test");

        store.dropCollection(COL);
        store.ensureCollection(COL, 4);
        waitDimension(store, COL, 4);
        store.upsert(COL, "a", new double[]{1, 0, 0, 0}, meta("alpha", "en"));
        store.upsert(COL, "b", new double[]{0, 1, 0, 0}, meta("beta", "fr"));

        VectorSearchResult all = searchEventually(store, VectorSearchRequest.builder()
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

        store.dropCollection(COL);
    }

    private static void waitDimension(ElasticsearchVectorStore store, String collection, int dim)
            throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            Integer current = store.collectionDimension(collection);
            if (current != null && current == dim) {
                return;
            }
            Thread.sleep(100L);
        }
    }

    /** ES 近实时刷新（默认 1s），写入后轮询至可检索或超时。 */
    private static VectorSearchResult searchEventually(ElasticsearchVectorStore store, VectorSearchRequest request)
            throws InterruptedException {
        VectorSearchResult result = VectorSearchResult.empty();
        for (int i = 0; i < 50; i++) {
            result = store.search(request);
            if (!result.isEmpty()) {
                return result;
            }
            Thread.sleep(100L);
        }
        return result;
    }
}
