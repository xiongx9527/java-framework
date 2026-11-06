package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.OkHttp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 真实 Milvus 冒烟测试：探测 /v2/vectordb/collections/list 可用才跑，否则跳过。
 * 通过 -Dmilvus.url / -Dmilvus.token 指定，token 形如 root:Milvus。
 */
class MilvusVectorStoreIntegrationTest {
    private static final String COL = "fw_native_test";

    private static String setting(String property, String env, String fallback) {
        String value = System.getProperty(property, System.getenv(env));
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static boolean restReady(String url, String token) {
        try {
            Map<String, String> headers = new LinkedHashMap<String, String>();
            if (token != null && !token.isEmpty()) {
                headers.put("Authorization", "Bearer " + token);
            }
            byte[] data = "{}".getBytes("UTF-8");
            OkHttp.Response response = OkHttp.send("POST",
                    url + "/v2/vectordb/collections/list", data, headers, 3000, 3000);
            return response.getCode() == 200;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    void upsertAndSearch() {
        String url = setting("milvus.url", "MILVUS_URL", "http://localhost:19530");
        String token = setting("milvus.token", "MILVUS_TOKEN", null);
        assumeTrue(restReady(url, token),
                "milvus v2 REST API not enabled at " + url + "; skipping live test");
        MilvusVectorStore store = new MilvusVectorStore(url, token, COL);
        store.dropCollection(COL);
        store.ensureCollection(COL, 4);
        store.upsert(COL, "a", new double[]{1, 0, 0, 0},
                Collections.<String, Object>singletonMap("content", "alpha"));
        store.upsert(COL, "b", new double[]{0, 1, 0, 0},
                Collections.<String, Object>singletonMap("content", "beta"));
        VectorSearchResult res = store.search(VectorSearchRequest.builder()
                .collection(COL).vector(new double[]{1, 0, 0, 0}).topK(2).build());
        assertFalse(res.isEmpty());
        assertEquals("a", res.getHits().get(0).getDocument().getId());
        store.dropCollection(COL);
    }
}
