package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.FakeHttpServer;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilvusVectorStoreTest {

    private static final String API = "/v2/vectordb";

    private static FakeHttpServer server(final boolean[] exists, final int[] dimension) throws Exception {
        return new FakeHttpServer(request -> {
            String path = request.path;
            if ("/healthz".equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'code':0,'message':'OK'}"));
            }
            if ((API + "/collections/describe").equals(path)) {
                if (!exists[0]) {
                    return FakeHttpServer.json(200, FakeHttpServer.body(
                            "{'code':100,'message':'collection not found'}"));
                }
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'code':0,'data':{'fields':[{'name':'id','type':'Int64'},"
                                + "{'name':'vector','type':'FloatVector','params':{'dim':" + dimension[0] + "}}]}}"));
            }
            if ((API + "/collections/create").equals(path)) {
                exists[0] = true;
                return FakeHttpServer.json(200, FakeHttpServer.body("{'code':0,'data':{}}"));
            }
            if ((API + "/collections/drop").equals(path)) {
                exists[0] = false;
                return FakeHttpServer.json(200, FakeHttpServer.body("{'code':0,'data':{}}"));
            }
            if ((API + "/entities/upsert").equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'code':0,'data':{'upsertCount':1}}"));
            }
            if ((API + "/entities/delete").equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'code':0,'data':{}}"));
            }
            if ((API + "/entities/search").equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'code':0,'data':["
                                + "{'id':111,'distance':0.99,'pk':'a','content':'alpha','lang':'en'},"
                                + "{'id':222,'distance':0.5,'pk':'b','content':'beta','lang':'fr'}]}"));
            }
            return FakeHttpServer.json(200, FakeHttpServer.body("{'code':0,'data':{}}"));
        });
    }

    @Test
    void healthEnsureCollectionAndDimension() throws Exception {
        final boolean[] exists = {false};
        FakeHttpServer server = server(exists, new int[]{4});
        try {
            MilvusVectorStore store = new MilvusVectorStore(server.url(), "root:Milvus", "col");
            assertTrue(store.health());
            store.ensureCollection("col", 4);
            FakeHttpServer.Request create = server.lastRequest();
            assertEquals(API + "/collections/create", create.path);
            assertEquals("Bearer root:Milvus", create.header("Authorization"));
            assertTrue(create.body.contains(FakeHttpServer.body("'collectionName':'col'")));
            assertTrue(create.body.contains(FakeHttpServer.body("'dimension':4")));
            assertTrue(create.body.contains(FakeHttpServer.body("'metricType':'COSINE'")));
            assertEquals(Integer.valueOf(4), store.collectionDimension("col"));
        } finally {
            server.close();
        }
    }

    @Test
    void upsertUsesSurrogateIdAndSearchRestoresStringId() throws Exception {
        final boolean[] exists = {false};
        FakeHttpServer server = server(exists, new int[]{4});
        try {
            MilvusVectorStore store = new MilvusVectorStore(server.url(), null, "col");
            store.ensureCollection("col", 4);
            store.upsert("col", "a", new double[]{1, 0, 0, 0},
                    Collections.<String, Object>singletonMap("content", "alpha"));
            FakeHttpServer.Request upsert = server.lastRequest();
            assertEquals(API + "/entities/upsert", upsert.path);
            assertTrue(upsert.body.contains(FakeHttpServer.body("'pk':'a'")));
            assertTrue(upsert.body.contains(FakeHttpServer.body("'vector':[1.0,0.0,0.0,0.0]")));
            assertFalse(upsert.body.contains(FakeHttpServer.body("'id':'a'")));

            VectorSearchResult all = store.search(VectorSearchRequest.builder()
                    .collection("col").vector(new double[]{1, 0, 0, 0}).topK(2).build());
            assertEquals(2, all.size());
            assertEquals("a", all.getHits().get(0).getDocument().getId());
            assertEquals("alpha", all.getHits().get(0).getDocument().getMetadata().get("content"));
            FakeHttpServer.Request search = server.lastRequest();
            assertEquals(API + "/entities/search", search.path);
            assertTrue(search.body.contains(FakeHttpServer.body("'outputFields':['*']")));

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
        } finally {
            server.close();
        }
    }

    @Test
    void deleteAndDropUseV2EndpointsAndIdFilter() throws Exception {
        final boolean[] exists = {true};
        FakeHttpServer server = server(exists, new int[]{4});
        try {
            MilvusVectorStore store = new MilvusVectorStore(server.url(), null, "col");
            store.delete("col", "a");
            assertEquals(API + "/entities/delete", server.lastRequest().path);
            assertTrue(server.lastRequest().body.contains("id in ["));
            store.deleteBatch("col", Arrays.asList("a", "b"));
            store.dropCollection("col");
            assertEquals(API + "/collections/drop", server.lastRequest().path);
            assertTrue(server.lastRequest().body.contains(FakeHttpServer.body("'collectionName':'col'")));
        } finally {
            server.close();
        }
    }

    @Test
    void nonZeroCodeIsTreatedAsFailure() throws Exception {
        FakeHttpServer server = new FakeHttpServer(request -> FakeHttpServer.json(200,
                FakeHttpServer.body("{'code':5,'message':'quota exceeded'}")));
        try {
            MilvusVectorStore store = new MilvusVectorStore(server.url(), null, "col");
            assertThrows(IllegalStateException.class, () -> store.ensureCollection("col", 4));
        } finally {
            server.close();
        }
    }
}
