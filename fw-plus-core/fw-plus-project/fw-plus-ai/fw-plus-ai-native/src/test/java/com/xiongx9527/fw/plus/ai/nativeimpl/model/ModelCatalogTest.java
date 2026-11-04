package com.xiongx9527.fw.plus.ai.nativeimpl.model;

import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelType;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.OpenAiCompatibleEmbeddingPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.FakeHttpServer;
import com.xiongx9527.fw.plus.ai.nativeimpl.llm.AnthropicLlmPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.llm.OpenAiCompatibleLlmPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.rerank.RerankModelPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCatalogTest {

    private static FakeHttpServer server() throws Exception {
        return new FakeHttpServer(request -> {
            String path = request.path;
            if ("/v1/models".equals(path) && "GET".equals(request.method)) {
                if (request.header("x-api-key") != null) {
                    return FakeHttpServer.json(200, FakeHttpServer.body(
                            "{'data':[{'id':'claude-3-5-sonnet','display_name':'Claude 3.5 Sonnet'}]}"));
                }
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'data':[{'id':'gpt-4o','owned_by':'openai'},"
                                + "{'id':'text-embedding-3-small','owned_by':'openai'},"
                                + "{'id':'bge-m3','owned_by':'local'}]}"));
            }
            if ("/v1/embeddings".equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'data':[{'embedding':[0.1,0.2,0.3]}]}"));
            }
            if ("/rerank".equals(path)) {
                if (request.body.contains("missing")) {
                    return FakeHttpServer.json(500, FakeHttpServer.body("{'error':'model not found'}"));
                }
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'results':[{'index':0,'relevance_score':0.9}]}"));
            }
            return FakeHttpServer.json(404, FakeHttpServer.body("{'error':'unexpected path'}"));
        });
    }

    @Test
    void openAiListsModelsAndValidatesAgainstCatalog() throws Exception {
        FakeHttpServer server = server();
        try {
            OpenAiCompatibleLlmPort port = new OpenAiCompatibleLlmPort(server.url() + "/v1", "k", "gpt-4o");
            List<ModelInfo> models = port.listModels();
            assertEquals(3, models.size());
            assertEquals("gpt-4o", models.get(0).getId());
            assertEquals("openai", models.get(0).getOwnedBy());
            assertEquals(ModelType.LLM, models.get(0).getType());
            assertEquals("Bearer k", server.lastRequest().header("Authorization"));

            ModelValidation hit = port.validateModel("gpt-4o");
            assertTrue(hit.isValid());
            assertFalse(port.validateModel("nope").isValid());
            assertFalse(port.validateModel(null).isValid());
        } finally {
            server.close();
        }
    }

    @Test
    void anthropicListsModelsWithDisplayNameAndVersionHeader() throws Exception {
        FakeHttpServer server = server();
        try {
            AnthropicLlmPort port = new AnthropicLlmPort(server.url(), "k", "claude-3-5-sonnet");
            List<ModelInfo> models = port.listModels();
            assertEquals(1, models.size());
            assertEquals("claude-3-5-sonnet", models.get(0).getId());
            assertEquals("Claude 3.5 Sonnet", models.get(0).getName());
            assertEquals("k", server.lastRequest().header("x-api-key"));
            assertEquals("2023-06-01", server.lastRequest().header("anthropic-version"));
            assertTrue(port.validateModel("claude-3-5-sonnet").isValid());
        } finally {
            server.close();
        }
    }

    @Test
    void embeddingFiltersCatalogByModelNameHints() throws Exception {
        FakeHttpServer server = server();
        try {
            OpenAiCompatibleEmbeddingPort port =
                    new OpenAiCompatibleEmbeddingPort(server.url() + "/v1", "k", "text-embedding-3-small");
            List<ModelInfo> models = port.listModels();
            assertEquals(2, models.size());
            assertEquals("text-embedding-3-small", models.get(0).getId());
            assertEquals(ModelType.EMBEDDING, models.get(0).getType());
            assertEquals("bge-m3", models.get(1).getId());

            ModelValidation result = port.validateModel("bge-m3");
            assertTrue(result.isValid());
            assertTrue(server.lastRequest().body.contains(FakeHttpServer.body("'model':'bge-m3'")));
            assertFalse(port.validateModel(" ").isValid());
        } finally {
            server.close();
        }
    }

    @Test
    void rerankListsConfiguredModelAndProbesScore() throws Exception {
        FakeHttpServer server = server();
        try {
            RerankModelPort port = new RerankModelPort(server.url() + "/rerank", null, "bge-reranker",
                    RerankModelPort.Style.COHERE);
            List<ModelInfo> models = port.listModels();
            assertEquals(1, models.size());
            assertEquals("bge-reranker", models.get(0).getId());
            assertEquals(ModelType.RERANK, models.get(0).getType());

            assertTrue(port.validateModel("bge-reranker").isValid());
            assertFalse(port.validateModel("missing").isValid());
        } finally {
            server.close();
        }
    }

    @Test
    void rerankWithoutConfiguredModelReturnsEmptyCatalog() throws Exception {
        FakeHttpServer server = server();
        try {
            RerankModelPort port = new RerankModelPort(server.url() + "/rerank", RerankModelPort.Style.TEI);
            assertTrue(port.listModels().isEmpty());
        } finally {
            server.close();
        }
    }
}
