package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.FakeHttpServer;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QdrantVectorStoreTest {

    private static FakeHttpServer server(final boolean[] exists, final int[] dimension) throws Exception {
        return new FakeHttpServer(request -> {
            String path = request.path;
            String method = request.method;
            if ("/healthz".equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'title':'qdrant'}"));
            }
            if ("/collections/col".equals(path) && "GET".equals(method)) {
                if (!exists[0]) {
                    return FakeHttpServer.json(404, FakeHttpServer.body("{'status':{'error':'not found'}}"));
                }
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'result':{'config':{'params':{'vectors':{'size':" + dimension[0] + ",'distance':'Cosine'}}}}}"));
            }
            if ("/collections/col".equals(path) && "PUT".equals(method)) {
                exists[0] = true;
                return FakeHttpServer.json(200, FakeHttpServer.body("{'result':true}"));
            }
            if ("/collections/col".equals(path) && "DELETE".equals(method)) {
                exists[0] = false;
                return FakeHttpServer.json(200, FakeHttpServer.body("{'result':true}"));
            }
            if ("/collections/col/points".equals(path) && "PUT".equals(method)) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'result':{'status':'completed'}}"));
            }
            if (path.endsWith("/points/search")) {
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'result':["
                                + "{'id':'11111111-1111-1111-1111-111111111111','score':0.99,"
                                + "'payload':{'pk':'a','content':'alpha','lang':'en'}},"
                                + "{'id':'22222222-2222-2222-2222-222222222222','score':0.5,"
                                + "'payload':{'pk':'b','content':'beta','lang':'fr'}}]}"));
            }
            if (path.endsWith("/points/delete")) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'result':{'status':'completed'}}"));
            }
            return FakeHttpServer.json(200, "{}");
        });
    }

    @Test
    void healthAndEnsureCollectionSendCosineSizeAndApiKey() throws Exception {
        final boolean[] exists = {false};
        FakeHttpServer server = server(exists, new int[]{4});
        try {
            QdrantVectorStore store = new QdrantVectorStore(server.url(), "secret", "col");
            assertTrue(store.health());
            store.ensureCollection("col", 4);
            FakeHttpServer.Request create = server.lastRequest();
            assertEquals("PUT", create.method);
            assertEquals("/collections/col", create.path);
            assertEquals("secret", create.header("api-key"));
            assertTrue(create.body.contains(FakeHttpServer.body("'vectors':{'size':4,'distance':'Cosine'}")));
            assertEquals(Integer.valueOf(4), store.collectionDimension("col"));
        } finally {
            server.close();
        }
    }

    @Test
    void upsertAndSearchWithCandidateAndMetadataFilters() throws Exception {
        final boolean[] exists = {false};
        FakeHttpServer server = server(exists, new int[]{4});
        try {
            QdrantVectorStore store = new QdrantVectorStore(server.url(), null, "col");
            store.ensureCollection("col", 4);
            store.upsert("col", "a", new double[]{1, 0, 0, 0},
                    Collections.<String, Object>singletonMap("content", "alpha"));
            FakeHttpServer.Request upsert = server.lastRequest();
            assertEquals("PUT", upsert.method);
            assertTrue(upsert.body.contains(FakeHttpServer.body("'vector':[1.0,0.0,0.0,0.0]")));
            assertTrue(upsert.body.contains(FakeHttpServer.body("'pk':'a'")));

            VectorSearchResult all = store.search(VectorSearchRequest.builder()
                    .collection("col").vector(new double[]{1, 0, 0, 0}).topK(2).build());
            assertFalse(all.isEmpty());
            assertEquals(2, all.size());
            assertEquals("a", all.getHits().get(0).getDocument().getId());
            assertEquals("alpha", all.getHits().get(0).getDocument().getMetadata().get("content"));
            assertTrue(server.lastRequest().body.contains(FakeHttpServer.body("'with_payload':true")));

            VectorSearchResult byCandidate = store.search(VectorSearchRequest.builder()
                    .collection("col").vector(new double[]{1, 0, 0, 0}).topK(2)
                    .candidateIds(Arrays.asList("b")).build());
            assertEquals(1, byCandidate.size());
            assertEquals("b", byCandidate.getHits().get(0).getDocument().getId());

            VectorSearchResult byFilter = store.search(VectorSearchRequest.builder()
                    .collection("col").vector(new double[]{1, 0, 0, 0}).topK(2)
                    .filter("lang", "en").build());
            assertEquals(1, byFilter.size());
            assertEquals("a", byFilter.getHits().get(0).getDocument().getId());

            store.delete("col", "a");
            assertEquals("POST", server.lastRequest().method);
            store.deleteBatch("col", Arrays.asList("a", "b"));
            store.dropCollection("col");
            assertEquals("DELETE", server.lastRequest().method);
        } finally {
            server.close();
        }
    }

    @Test
    void dimensionMismatchIsRejected() throws Exception {
        final boolean[] exists = {true};
        FakeHttpServer server = server(exists, new int[]{4});
        try {
            QdrantVectorStore store = new QdrantVectorStore(server.url(), null, "col");
            assertThrows(VectorStorePort.DimensionMismatchException.class,
                    () -> store.ensureCollection("col", 8));
        } finally {
            server.close();
        }
    }
}
