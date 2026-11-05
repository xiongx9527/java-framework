package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.FakeHttpServer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeaviateVectorStoreTest {

    private static final ObjectMapper M = new ObjectMapper();

    private static String graphqlResponse() throws Exception {
        Map<String, Object> meta1 = new LinkedHashMap<String, Object>();
        meta1.put("content", "alpha");
        meta1.put("lang", "en");
        Map<String, Object> obj1 = new LinkedHashMap<String, Object>();
        obj1.put("content", "alpha");
        obj1.put("metadata", M.writeValueAsString(meta1));
        obj1.put("pk", "a");
        Map<String, Object> add1 = new LinkedHashMap<String, Object>();
        add1.put("id", "u1");
        add1.put("certainty", 0.92);
        obj1.put("_additional", add1);

        Map<String, Object> meta2 = new LinkedHashMap<String, Object>();
        meta2.put("content", "beta");
        meta2.put("lang", "fr");
        Map<String, Object> obj2 = new LinkedHashMap<String, Object>();
        obj2.put("content", "beta");
        obj2.put("metadata", M.writeValueAsString(meta2));
        obj2.put("pk", "b");
        Map<String, Object> add2 = new LinkedHashMap<String, Object>();
        add2.put("id", "u2");
        add2.put("certainty", 0.35);
        obj2.put("_additional", add2);

        Map<String, Object> get = new LinkedHashMap<String, Object>();
        get.put("Col", Arrays.asList(obj1, obj2));
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("Get", get);
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("data", data);
        return M.writeValueAsString(root);
    }

    private static FakeHttpServer server(final boolean[] exists) throws Exception {
        return new FakeHttpServer(request -> {
            String path = request.path;
            String method = request.method;
            if ("/v1/meta".equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'version':'1.25.0'}"));
            }
            if ("/v1/schema/Col".equals(path) && "GET".equals(method)) {
                if (!exists[0]) {
                    return FakeHttpServer.json(404, FakeHttpServer.body("{'error':[{'message':'not found'}]}"));
                }
                return FakeHttpServer.json(200, FakeHttpServer.body("{'class':'Col'}"));
            }
            if ("/v1/schema".equals(path) && "POST".equals(method)) {
                exists[0] = true;
                return FakeHttpServer.json(200, FakeHttpServer.body("{'class':'Col'}"));
            }
            if ("/v1/schema/Col".equals(path) && "DELETE".equals(method)) {
                exists[0] = false;
                return FakeHttpServer.json(200, "");
            }
            if ("/v1/objects".equals(path) && "POST".equals(method)) {
                return FakeHttpServer.json(200, FakeHttpServer.body("{'id':'x'}"));
            }
            if (path.startsWith("/v1/objects/Col/") && "DELETE".equals(method)) {
                return FakeHttpServer.json(200, "");
            }
            if ("/v1/graphql".equals(path)) {
                try {
                    return FakeHttpServer.json(200, graphqlResponse());
                } catch (Exception ex) {
                    return FakeHttpServer.json(500, FakeHttpServer.body("{'error':'build failed'}"));
                }
            }
            return FakeHttpServer.json(200, "{}");
        });
    }

    @Test
    void healthAndEnsureCollectionCreatesClassWithoutVectorizer() throws Exception {
        final boolean[] exists = {false};
        FakeHttpServer server = server(exists);
        try {
            WeaviateVectorStore store = new WeaviateVectorStore(server.url(), "secret", "col");
            assertTrue(store.health());
            store.ensureCollection("col", 4);
            FakeHttpServer.Request create = server.lastRequest();
            assertEquals("POST", create.method);
            assertEquals("/v1/schema", create.path);
            assertEquals("Bearer secret", create.header("authorization"));
            assertTrue(create.body.contains(FakeHttpServer.body("'class':'Col'")));
            assertTrue(create.body.contains(FakeHttpServer.body("'vectorizer':'none'")));
        } finally {
            server.close();
        }
    }

    @Test
    void upsertAndSearchWithCandidateAndMetadataFilters() throws Exception {
        final boolean[] exists = {true};
        FakeHttpServer server = server(exists);
        try {
            WeaviateVectorStore store = new WeaviateVectorStore(server.url(), null, "col");
            store.upsert("col", "a", new double[]{1, 0, 0, 0},
                    Collections.<String, Object>singletonMap("content", "alpha"));
            FakeHttpServer.Request upsert = server.lastRequest();
            assertEquals("POST", upsert.method);
            assertEquals("/v1/objects", upsert.path);
            assertTrue(upsert.body.contains(FakeHttpServer.body("'class':'Col'")));
            assertTrue(upsert.body.contains(FakeHttpServer.body("'pk':'a'")));

            VectorSearchResult all = store.search(VectorSearchRequest.builder()
                    .collection("col").vector(new double[]{1, 0, 0, 0}).topK(2).build());
            assertFalse(all.isEmpty());
            assertEquals(2, all.size());
            assertEquals("a", all.getHits().get(0).getDocument().getId());
            assertEquals("alpha", all.getHits().get(0).getDocument().getMetadata().get("content"));
            assertEquals(0.92, all.getHits().get(0).getScore(), 0.0001);

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
}
