package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class LangChain4jEmbeddingPortTest {

    private static class StubEmbeddingModel implements EmbeddingModel {

        @Override
        public Response<Embedding> embed(String text) {
            return Response.from(Embedding.from(new float[]{1.0f, 2.0f, 3.0f}));
        }

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            List<Embedding> out = new ArrayList<Embedding>();
            for (int i = 0; i < segments.size(); i++) {
                out.add(Embedding.from(new float[]{i, i + 1, i + 2}));
            }
            return Response.from(out);
        }

        @Override
        public int dimension() {
            return 3;
        }
    }

    @Test
    void singleEmbeddingConvertsFloatToDouble() {
        EmbeddingPort port = new LangChain4jEmbeddingPort(new StubEmbeddingModel());
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, port.embed("x"), 0.0001);
        assertEquals(3, port.dimension());
        assertNull(port.embed(null));
    }

    @Test
    void batchEmbeddingUsesUpstreamBatchApi() {
        EmbeddingPort port = new LangChain4jEmbeddingPort(new StubEmbeddingModel());
        List<double[]> vectors = port.embedAll(Arrays.asList("a", "b"));
        assertEquals(2, vectors.size());
        assertArrayEquals(new double[]{0.0, 1.0, 2.0}, vectors.get(0), 0.0001);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, vectors.get(1), 0.0001);
    }

    @Test
    void validateModelProbesWithRealEmbedding() {
        EmbeddingPort port = new LangChain4jEmbeddingPort(new StubEmbeddingModel());
        assertTrue(port.validateModel("text-embedding-3-small").isValid());
    }
}
