package com.xiongx9527.fw.plus.ai.nativeimpl.rerank;

import com.xiongx9527.fw.plus.ai.nativeimpl.support.FakeHttpServer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RerankModelPortTest {

    @Test
    void teiStyleRealignsDescendingScoresToInputOrder() throws Exception {
        FakeHttpServer server = new FakeHttpServer(request -> FakeHttpServer.json(200, FakeHttpServer.body(
                "[{'index':1,'score':0.9},{'index':0,'score':0.1}]")));
        try {
            RerankModelPort port = new RerankModelPort(server.url() + "/rerank", RerankModelPort.Style.TEI);
            List<Double> scores = port.score("q", Arrays.asList("docA", "docB"));
            assertEquals(2, scores.size());
            assertEquals(0.1, scores.get(0), 1e-9);
            assertEquals(0.9, scores.get(1), 1e-9);
            FakeHttpServer.Request request = server.lastRequest();
            assertEquals("POST", request.method);
            assertEquals("/rerank", request.path);
            assertTrue(request.body.contains(FakeHttpServer.body("'query':'q'")));
            assertTrue(request.body.contains(FakeHttpServer.body("'texts':['docA','docB']")));
        } finally {
            server.close();
        }
    }

    @Test
    void cohereStyleSendsBearerAndRealignsResults() throws Exception {
        FakeHttpServer server = new FakeHttpServer(request -> FakeHttpServer.json(200, FakeHttpServer.body(
                "{'results':[{'index':2,'relevance_score':0.7},{'index':0,'relevance_score':0.4}]}")));
        try {
            RerankModelPort port = new RerankModelPort(server.url() + "/v1/rerank", "k", "rerank-v3",
                    RerankModelPort.Style.COHERE);
            List<Double> scores = port.score("q", Arrays.asList("a", "b", "c"));
            assertEquals(3, scores.size());
            assertEquals(0.4, scores.get(0), 1e-9);
            assertEquals(0.0, scores.get(1), 1e-9);
            assertEquals(0.7, scores.get(2), 1e-9);
            FakeHttpServer.Request request = server.lastRequest();
            assertEquals("Bearer k", request.header("Authorization"));
            assertTrue(request.body.contains(FakeHttpServer.body("'model':'rerank-v3'")));
            assertTrue(request.body.contains(FakeHttpServer.body("'documents':['a','b','c']")));
            assertTrue(request.body.contains(FakeHttpServer.body("'top_n':3")));
        } finally {
            server.close();
        }
    }

    @Test
    void emptyDocumentsReturnsEmptyListWithoutCallingServer() throws Exception {
        FakeHttpServer server = new FakeHttpServer(request -> FakeHttpServer.json(200, "[]"));
        try {
            RerankModelPort port = new RerankModelPort(server.url() + "/rerank", RerankModelPort.Style.TEI);
            assertTrue(port.score("q", null).isEmpty());
            assertTrue(server.requests().isEmpty());
        } finally {
            server.close();
        }
    }

    @Test
    void serverFailureFallsBackToEqualLengthZeroScores() throws Exception {
        FakeHttpServer server = new FakeHttpServer(request -> FakeHttpServer.json(500,
                FakeHttpServer.body("{'error':'boom'}")));
        try {
            RerankModelPort port = new RerankModelPort(server.url() + "/rerank", RerankModelPort.Style.COHERE);
            List<Double> scores = port.score("q", Arrays.asList("a", "b"));
            assertEquals(2, scores.size());
            assertEquals(0.0, scores.get(0), 1e-9);
            assertEquals(0.0, scores.get(1), 1e-9);
        } finally {
            server.close();
        }
    }
}
