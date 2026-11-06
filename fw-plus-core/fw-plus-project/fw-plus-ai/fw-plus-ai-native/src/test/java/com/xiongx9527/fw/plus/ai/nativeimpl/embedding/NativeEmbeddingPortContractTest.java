package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import com.xiongx9527.fw.plus.ai.nativeimpl.http.FakeHttpServer;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.FakeHttpServer.Request;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.FakeHttpServer.Response;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用进程内 HTTP 桩验证 Cohere / Azure / TEI 三个原生向量端口的请求形态与响应解析，
 * 不依赖外部服务（native 后端的契约测试约定）。
 */
class NativeEmbeddingPortContractTest {

    private FakeHttpServer server;

    @BeforeEach
    void start() throws Exception {
        server = new FakeHttpServer(request -> {
            if (request.path.equals("/v1/embed")) {
                // Cohere
                assertTrue(request.header("authorization").startsWith("Bearer "));
                return FakeHttpServer.json(200, FakeHttpServer.body("{'embeddings':{'float':[[0.1,0.2]]}}"));
            }
            if (request.path.contains("/openai/deployments/")) {
                // Azure OpenAI
                assertTrue(request.header("api-key") != null && !request.header("api-key").isEmpty());
                assertTrue(request.path.contains("api-version=2023-05-15"));
                return FakeHttpServer.json(200, FakeHttpServer.body("{'data':[{'embedding':[0.3,0.4]}]}"));
            }
            if (request.path.equals("/embed")) {
                // TEI
                return FakeHttpServer.json(200, FakeHttpServer.body("{'embeddings':[[0.5,0.6]]}"));
            }
            if (request.path.equals("/info")) {
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'model_id':'tei-model','model_type':{'embedding':{'pooling':'cls'}}}"));
            }
            return FakeHttpServer.json(404, "{}");
        });
    }

    @AfterEach
    void stop() throws Exception {
        server.close();
    }

    @Test
    void cohereParsesFloatEmbeddings() {
        CohereEmbeddingPort port = new CohereEmbeddingPort(server.url(), "k", "model");
        List<double[]> vectors = port.embedAll(Collections.singletonList("hello"));
        assertEquals(1, vectors.size());
        assertArrayEquals(new double[] { 0.1, 0.2 }, vectors.get(0), 1e-9);
    }

    @Test
    void azureParsesDataEmbeddings() {
        AzureOpenAiEmbeddingPort port = new AzureOpenAiEmbeddingPort(server.url(), "k", "deployment");
        List<double[]> vectors = port.embedAll(Collections.singletonList("hello"));
        assertEquals(1, vectors.size());
        assertArrayEquals(new double[] { 0.3, 0.4 }, vectors.get(0), 1e-9);
    }

    @Test
    void teiParsesEmbeddingsAndInfo() {
        TeiEmbeddingPort port = new TeiEmbeddingPort(server.url(), "k", "tei-model");
        List<double[]> vectors = port.embedAll(Collections.singletonList("hello"));
        assertEquals(1, vectors.size());
        assertArrayEquals(new double[] { 0.5, 0.6 }, vectors.get(0), 1e-9);
        assertTrue(port.listModels().stream().anyMatch(m -> "tei-model".equals(m.getId())));
    }
}
