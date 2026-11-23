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
    void cohereStyleSendsSpecFieldsAndAcceptsScoreAlias() throws Exception {
        // §3 规范：请求带 return_documents；响应 score 别名也要能读（§3.5 厂商变体）
        FakeHttpServer server = new FakeHttpServer(request -> FakeHttpServer.json(200, FakeHttpServer.body(
                "{'results':[{'index':1,'score':0.9},{'index':0,'score':0.2}],'usage':{'total_tokens':8}}")));
        try {
            RerankModelPort port = new RerankModelPort(server.url() + "/rerank/v1/rerank", "k",
                    "bge-reranker-v2-m3", RerankModelPort.Style.COHERE);
            List<Double> scores = port.score("q", Arrays.asList("docA", "docB"));
            assertEquals(2, scores.size());
            assertEquals(0.2, scores.get(0), 1e-9);
            assertEquals(0.9, scores.get(1), 1e-9);
            FakeHttpServer.Request request = server.lastRequest();
            assertEquals("POST", request.method);
            assertEquals("/rerank/v1/rerank", request.path);
            assertTrue(request.body.contains(FakeHttpServer.body("'model':'bge-reranker-v2-m3'")));
            assertTrue(request.body.contains(FakeHttpServer.body("'documents':['docA','docB']")));
            assertTrue(request.body.contains(FakeHttpServer.body("'top_n':2")));
            assertTrue(request.body.contains(FakeHttpServer.body("'return_documents':false")));
        } finally {
            server.close();
        }
    }

    @Test
    void cohereStyleAppendsSpecPathWhenEndpointIsBaseOnly() throws Exception {
        FakeHttpServer server = new FakeHttpServer(request -> FakeHttpServer.json(200, FakeHttpServer.body(
                "{'results':[{'index':0,'relevance_score':0.5}]}")));
        try {
            // 只给服务根：COHERE 规范端点应补 /rerank/v1/rerank
            RerankModelPort port = new RerankModelPort(server.url(), "k", "m",
                    RerankModelPort.Style.COHERE);
            port.score("q", Arrays.asList("a"));
            assertEquals("/rerank/v1/rerank", server.lastRequest().path);
        } finally {
            server.close();
        }
    }

    @Test
    void cohereStyleAppendsRerankPathWhenEndpointIsBaseOnly() throws Exception {
        FakeHttpServer server = new FakeHttpServer(request -> FakeHttpServer.json(200, FakeHttpServer.body(
                "{'results':[{'index':0,'relevance_score':0.5}]}")));
        try {
            // endpoint 只给服务根，框架应自动补统一规范路径 /rerank/v1/rerank
            RerankModelPort port = new RerankModelPort(server.url(), "k", "rerank-v3",
                    RerankModelPort.Style.COHERE);
            port.score("q", Arrays.asList("a"));
            FakeHttpServer.Request req1 = server.lastRequest();
            assertEquals("/rerank/v1/rerank", req1.path);
            // 已带 /v1 时只补 /rerank，避免 /v1/v1/rerank
            FakeHttpServer server2 = new FakeHttpServer(request -> FakeHttpServer.json(200, FakeHttpServer.body(
                    "{'results':[{'index':0,'relevance_score':0.5}]}")));
            try {
                RerankModelPort port2 = new RerankModelPort(server2.url() + "/v1", "k", "rerank-v3",
                        RerankModelPort.Style.COHERE);
                port2.score("q", Arrays.asList("a"));
                assertEquals("/v1/rerank", server2.lastRequest().path);
            } finally {
                server2.close();
            }
        } finally {
            server.close();
        }
    }

    @Test
    void fullRerankPathIsKeptAsIs() throws Exception {
        FakeHttpServer server = new FakeHttpServer(request -> FakeHttpServer.json(200, FakeHttpServer.body(
                "{'results':[{'index':0,'relevance_score':0.5}]}")));
        try {
            // 已给全新路径 /rerank/v1/rerank：框架不应再追加，原样请求
            RerankModelPort port = new RerankModelPort(server.url() + "/rerank/v1/rerank", "k", "rerank-v3",
                    RerankModelPort.Style.COHERE);
            port.score("q", Arrays.asList("a"));
            assertEquals("/rerank/v1/rerank", server.lastRequest().path);
            // TEI 遵循其官方规范路径 /rerank，同样原样保留（不套本项目 /v1/{资源} 格式）
            FakeHttpServer server2 = new FakeHttpServer(request -> FakeHttpServer.json(200, FakeHttpServer.body(
                    "[{'index':0,'score':0.5}]")));
            try {
                RerankModelPort tei = new RerankModelPort(server2.url() + "/rerank", "k", null,
                        RerankModelPort.Style.TEI);
                tei.score("q", Arrays.asList("a"));
                assertEquals("/rerank", server2.lastRequest().path);
            } finally {
                server2.close();
            }
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
