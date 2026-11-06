package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelType;
import com.xiongx9527.fw.plus.ai.nativeimpl.support.FakeHttpServer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用进程内 HTTP 桩（okhttp3 MockWebServer 实现）验证 OpenAiCompatibleEmbeddingPort 的模型枚举：
 * 优先 /models，404 时回退 /info。
 */
class OpenAiCompatibleEmbeddingPortCatalogTest {

    private FakeHttpServer server;
    private String baseUrl;
    private final AtomicInteger infoRequests = new AtomicInteger();
    private final Map<String, FakeHttpServer.Response> responses = new LinkedHashMap<String, FakeHttpServer.Response>();

    @BeforeEach
    void startServer() throws Exception {
        server = new FakeHttpServer(request -> {
            if ("/info".equals(request.path)) {
                infoRequests.incrementAndGet();
            }
            FakeHttpServer.Response r = responses.get(request.path);
            return r != null ? r : FakeHttpServer.json(404, "{}");
        });
        baseUrl = server.url();
    }

    @AfterEach
    void stopServer() throws Exception {
        server.close();
    }

    @Test
    void usesModelsBeforeInfo() {
        responses.put("/models", FakeHttpServer.json(200, FakeHttpServer.body(
                "{'data':[{'id':'bge-m3','owned_by':'local'}]}")));
        responses.put("/info", FakeHttpServer.json(200, infoResponse()));

        List<ModelInfo> models = port(baseUrl).listModels();

        assertEquals(1, models.size());
        assertEquals("bge-m3", models.get(0).getId());
        assertEquals(ModelType.EMBEDDING, models.get(0).getType());
        assertEquals(0, infoRequests.get());
    }

    @Test
    void fallsBackToRootInfoWhenModelsReturnsNotFound() {
        responses.put("/v1/models", FakeHttpServer.json(404, "not found"));
        responses.put("/info", FakeHttpServer.json(200, infoResponse()));

        List<ModelInfo> models = port(baseUrl + "/v1").listModels();

        assertEquals(1, models.size());
        assertEquals("/models/bge-m3", models.get(0).getId());
        assertEquals(ModelType.EMBEDDING, models.get(0).getType());
        assertEquals(1, infoRequests.get());
    }

    @Test
    void doesNotFallBackToInfoForNonNotFoundErrors() {
        responses.put("/models", FakeHttpServer.json(500, "server error"));
        responses.put("/info", FakeHttpServer.json(200, infoResponse()));

        List<ModelInfo> models = port(baseUrl).listModels();

        assertTrue(models.isEmpty());
        assertEquals(0, infoRequests.get());
    }

    private OpenAiCompatibleEmbeddingPort port(String url) {
        return new OpenAiCompatibleEmbeddingPort(url, null, "/models/bge-m3");
    }

    private String infoResponse() {
        return "{\"model_id\":\"/models/bge-m3\",\"model_sha\":null,"
                + "\"model_dtype\":\"float32\",\"served_model_name\":\"/models/bge-m3\","
                + "\"model_type\":{\"embedding\":{\"pooling\":\"cls\"}},"
                + "\"max_input_length\":8192,\"version\":\"1.9.4\"}";
    }
}
