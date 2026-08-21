package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbeddingStoreAskDataVectorRetrieverTest {

    @Test
    public void shouldRetrieveFromLangChain4jEmbeddingStore() {
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        store.add(Embedding.from(new float[] {1.0F, 0.0F}), TextSegment.from("GMV means amount sum"));
        EmbeddingModel model = new EmbeddingModel() {
            @Override
            public Response<Embedding> embed(String text) {
                return Response.from(Embedding.from(new float[] {1.0F, 0.0F}));
            }
        };
        AskDataVectorRetriever retriever = new EmbeddingStoreAskDataVectorRetriever(store, model);

        assertEquals("GMV means amount sum", retriever.retrieve(new AskDataVectorQuery("term", "orders", "shared",
                "GMV", 1, Collections.<String, Object>emptyMap())).get(0).getContent());
    }

    @Test
    public void shouldRejectFiltersThatLegacyLangChain4jStoreCannotApply() {
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        EmbeddingModel model = texts -> Response.from(
                Collections.singletonList(Embedding.from(new float[] {1.0F, 0.0F})));
        AskDataVectorRetriever retriever = new EmbeddingStoreAskDataVectorRetriever(store, model);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> retriever.retrieve(new AskDataVectorQuery("term", "orders", "shared", "GMV", 1,
                        Collections.<String, Object>singletonMap("projectId", "project-1"))));

        assertTrue(exception.getMessage().contains("授权过滤"));
    }
}
