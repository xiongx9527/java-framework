package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 真实 Qdrant 冒烟测试：默认跳过，需通过系统属性/环境变量给出地址与 apiKey 才跑。
 * 例如 -Dqdrant.apiKey=xxx（本机 brew 服务的 key 在 launchagent plist 里）。
 */
class QdrantVectorStoreIntegrationTest {
    private static final String COL = "fw_native_test";

    private static String setting(String property, String env, String fallback) {
        String value = System.getProperty(property, System.getenv(env));
        return value == null || value.isEmpty() ? fallback : value;
    }

    @Test
    void upsertAndSearch() {
        String url = setting("qdrant.url", "QDRANT_URL", "http://localhost:6333");
        String apiKey = setting("qdrant.apiKey", "QDRANT_API_KEY", null);
        assumeTrue(apiKey != null, "qdrant.apiKey/QDRANT_API_KEY not set; skipping live test");
        QdrantVectorStore store = new QdrantVectorStore(url, apiKey, COL);
        assumeTrue(store.health(), "qdrant not reachable at " + url);
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
        Document doc = res.getHits().get(0).getDocument();
        assertEquals("alpha", doc.getMetadata().get("content"));
        store.dropCollection(COL);
    }
}
