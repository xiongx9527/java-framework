package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 真实 Redis Stack（RediSearch）冒烟测试：探测 PING 可用才跑，否则跳过。
 * 通过 -Dredis.url / REDIS_URL、-Dredis.password / REDIS_PASSWORD 指定。
 */
class RedisVectorStoreIntegrationTest {
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
        String url = setting("redis.url", "REDIS_URL", "redis://localhost:6380");
        String password = setting("redis.password", "REDIS_PASSWORD", null);
        RedisVectorStore store = new RedisVectorStore(url, password, COL);
        assumeTrue(store.health(), "redis stack not reachable at " + url + "; skipping live test");

        store.delete(COL, "a");
        store.delete(COL, "b");
        store.dropCollection(COL);
        store.ensureCollection(COL, 4);
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
                .filter("lang", "en").build());
        assertEquals(1, byFilter.size());
        assertEquals("a", byFilter.getHits().get(0).getDocument().getId());

        store.dropCollection(COL);
        store.delete(COL, "a");
        store.delete(COL, "b");
    }

    /** RediSearch 建索引近实时，写入后可能短暂不可检索；轮询至出现结果或超时。 */
    private static VectorSearchResult searchEventually(RedisVectorStore store, VectorSearchRequest request)
            throws InterruptedException {
        VectorSearchResult result = VectorSearchResult.empty();
        for (int i = 0; i < 30; i++) {
            result = store.search(request);
            if (!result.isEmpty()) {
                return result;
            }
            Thread.sleep(100L);
        }
        return result;
    }
}
