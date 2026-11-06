package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import com.xiongx9527.fw.plus.ai.nativeimpl.support.FakeHttpServer;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ElasticsearchVectorStoreTest {

    private static FakeHttpServer server(final boolean[] exists, final int[] dimension) throws Exception {
        return new FakeHttpServer(request -> {
            String path = request.path;
            String method = request.method;
            if ("/_cluster/health".equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'status':'green'}"));
            }
            if ("/col".equals(path) && "GET".equals(method)) {
                if (!exists[0]) {
                    return FakeHttpServer.json(404, FakeHttpServer.body("{'error':'index_not_found'}"));
                }
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'mappings':{'properties':{'vector':{'type':'dense_vector','dims':" + dimension[0]
                                + ",'similarity':'cosine'}}}}"));
            }
            if ("/col".equals(path) && "PUT".equals(method)) {
                exists[0] = true;
                return FakeHttpServer.json(200, FakeHttpServer.body("{'acknowledged':true}"));
            }
            if ("/col".equals(path) && "DELETE".equals(method)) {
                exists[0] = false;
                return FakeHttpServer.json(200, FakeHttpServer.body("{'acknowledged':true}"));
            }
            if (path.startsWith("/col/_doc/") && "PUT".equals(method)) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'result':'created'}"));
            }
            if (path.startsWith("/col/_doc/") && "DELETE".equals(method)) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'result':'deleted'}"));
            }
            if ("/col/_search".equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'hits':{'hits':["
                                + "{'_id':'a','_score':0.95,'_source':{'metadata':{'content':'alpha','lang':'en'}}},"
                                + "{'_id':'b','_score':0.4,'_source':{'metadata':{'content':'beta','lang':'fr'}}}"
                                + "]}}"));
            }
            return FakeHttpServer.json(200, "{}");
        });
    }

    @Test
    void healthAndEnsureCollectionCreatesDenseVectorIndex() throws Exception {
        final boolean[] exists = {false};
        FakeHttpServer server = server(exists, new int[]{4});
        try {
            ElasticsearchVectorStore store = new ElasticsearchVectorStore(server.url(), "secret", "col");
            assertTrue(store.health());
            store.ensureCollection("col", 4);
            FakeHttpServer.Request create = server.lastRequest();
            assertEquals("PUT", create.method);
            assertEquals("/col", create.path);
            assertEquals("Bearer secret", create.header("authorization"));
            assertTrue(create.body.contains(FakeHttpServer.body("'dense_vector'")));
            assertTrue(create.body.contains(FakeHttpServer.body("'dims':4")));
            assertEquals(Integer.valueOf(4), store.collectionDimension("col"));
        } finally {
            server.close();
        }
    }

    @Test
    void upsertAndSearchWithCandidateAndMetadataFilters() throws Exception {
        final boolean[] exists = {true};
        FakeHttpServer server = server(exists, new int[]{4});
        try {
            ElasticsearchVectorStore store = new ElasticsearchVectorStore(server.url(), null, "col");
            store.upsert("col", "a", new double[]{1, 0, 0, 0},
                    Collections.<String, Object>singletonMap("content", "alpha"));
            FakeHttpServer.Request upsert = server.lastRequest();
            assertEquals("PUT", upsert.method);
            assertEquals("/col/_doc/a?refresh=wait_for", upsert.path);
            assertTrue(upsert.body.contains(FakeHttpServer.body("'vector':[1.0,0.0,0.0,0.0]")));

            VectorSearchResult all = store.search(VectorSearchRequest.builder()
                    .collection("col").vector(new double[]{1, 0, 0, 0}).topK(2).build());
            assertFalse(all.isEmpty());
            assertEquals(2, all.size());
            assertEquals("a", all.getHits().get(0).getDocument().getId());
            assertEquals("alpha", all.getHits().get(0).getDocument().getMetadata().get("content"));

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
            assertEquals("DELETE", server.lastRequest().method);
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
            ElasticsearchVectorStore store = new ElasticsearchVectorStore(server.url(), null, "col");
            assertThrows(VectorStorePort.DimensionMismatchException.class,
                    () -> store.ensureCollection("col", 8));
        } finally {
            server.close();
        }
    }
}
