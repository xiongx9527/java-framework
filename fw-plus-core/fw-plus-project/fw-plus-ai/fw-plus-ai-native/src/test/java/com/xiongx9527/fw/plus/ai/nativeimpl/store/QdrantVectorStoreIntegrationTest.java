package com.xiongx9527.fw.plus.ai.nativeimpl.store;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class QdrantVectorStoreIntegrationTest {
    private static final String COL = "fw_native_test";
    @Test
    void upsertAndSearch() {
        QdrantVectorStore store = new QdrantVectorStore("http://localhost:6333");
        assumeTrue(store.health(), "qdrant not reachable");
        store.dropCollection(COL);
        store.ensureCollection(COL, 4);
        store.upsert(COL, "a", new double[]{1, 0, 0, 0}, Collections.singletonMap("content", "alpha"));
        store.upsert(COL, "b", new double[]{0, 1, 0, 0}, Collections.singletonMap("content", "beta"));
        VectorSearchResult res = store.search(VectorSearchRequest.builder().collection(COL).vector(new double[]{1, 0, 0, 0}).topK(2).build());
        assertFalse(res.isEmpty());
        assertEquals("a", res.getHits().get(0).getDocument().getId());
        Document doc = res.getHits().get(0).getDocument();
        assertEquals("alpha", doc.getMetadata().get("content"));
        store.dropCollection(COL);
        assertTrue(true);
    }
}
