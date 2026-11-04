package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import com.sun.net.httpserver.HttpServer;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelType;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleEmbeddingPortCatalogTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger infoRequests = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void usesModelsBeforeInfo() {
        respond("/models", 200,
                "{\"data\":[{\"id\":\"bge-m3\",\"owned_by\":\"local\"}]}");
        respond("/info", 200, infoResponse());

        List<ModelInfo> models = port(baseUrl).listModels();

        assertEquals(1, models.size());
        assertEquals("bge-m3", models.get(0).getId());
        assertEquals(ModelType.EMBEDDING, models.get(0).getType());
        assertEquals(0, infoRequests.get());
    }

    @Test
    void fallsBackToRootInfoWhenModelsReturnsNotFound() {
        respond("/v1/models", 404, "not found");
        respond("/info", 200, infoResponse());

        List<ModelInfo> models = port(baseUrl + "/v1").listModels();

        assertEquals(1, models.size());
        assertEquals("/models/bge-m3", models.get(0).getId());
        assertEquals(ModelType.EMBEDDING, models.get(0).getType());
        assertEquals(1, infoRequests.get());
    }

    @Test
    void doesNotFallBackToInfoForNonNotFoundErrors() {
        respond("/models", 500, "server error");
        respond("/info", 200, infoResponse());

        List<ModelInfo> models = port(baseUrl).listModels();

        assertTrue(models.isEmpty());
        assertEquals(0, infoRequests.get());
    }

    private OpenAiCompatibleEmbeddingPort port(String url) {
        return new OpenAiCompatibleEmbeddingPort(url, null, "/models/bge-m3");
    }

    private void respond(String path, final int status, final String body) {
        server.createContext(path, exchange -> {
            if ("/info".equals(path)) {
                infoRequests.incrementAndGet();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
    }

    private String infoResponse() {
        return "{\"model_id\":\"/models/bge-m3\",\"model_sha\":null,"
                + "\"model_dtype\":\"float32\",\"served_model_name\":\"/models/bge-m3\","
                + "\"model_type\":{\"embedding\":{\"pooling\":\"cls\"}},"
                + "\"max_input_length\":8192,\"version\":\"1.9.4\"}";
    }
}
