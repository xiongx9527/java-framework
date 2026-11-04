package com.xiongx9527.fw.plus.ai.askdata.recall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.base.retrieval.FullTextRetriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.support.retrieval.IndexMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AskDataRetrieverTest {

    @Test
    void searchesOnlyProvidedSchemaCandidates() {
        AskDataRetriever retriever = new AskDataRetriever(
                query -> Arrays.asList(
                        TableSchema.builder().name("users").comment("user account").build(),
                        TableSchema.builder().name("orders").comment("order amount").build()),
                new FullTextRetriever());

        List<Retrieved<Document>> result = retriever.retrieve(
                RetrievalQuery.builder().text("user account").build());

        assertEquals(1, result.size());
        assertEquals("users", result.get(0).getItem().getId());
        assertTrue(result.get(0).getItem().getContent().contains("user account"));
    }

    @Test
    void filtersOutDocumentsNotInProvidedCandidates() {
        Retriever<Document> delegate = query -> Arrays.asList(
                Retrieved.of(Document.of("users", "allowed"), 0.9),
                Retrieved.of(Document.of("secret", "not allowed"), 0.8));
        AskDataRetriever retriever = new AskDataRetriever(
                query -> Collections.singletonList(TableSchema.builder().name("users").build()), delegate);

        List<Retrieved<Document>> result = retriever.retrieve(
                RetrievalQuery.builder().text("users").build());

        assertEquals(1, result.size());
        assertEquals("users", result.get(0).getItem().getId());
    }

    @Test
    void emptyCandidatesDoNotCallDelegate() {
        AtomicBoolean called = new AtomicBoolean();
        Retriever<Document> delegate = query -> {
            called.set(true);
            return Collections.emptyList();
        };
        AskDataRetriever retriever = new AskDataRetriever(query -> Collections.emptyList(), delegate);

        assertTrue(retriever.retrieve(RetrievalQuery.builder().text("users").build()).isEmpty());
        assertTrue(!called.get());
    }

    @Test
    void economicalStrategyUsesLexicalRetrieverOnly() {
        AtomicBoolean vectorCalled = new AtomicBoolean();
        Retriever<Document> vector = query -> {
            vectorCalled.set(true);
            return Collections.emptyList();
        };
        Retriever<Document> lexical = query -> Collections.singletonList(
                Retrieved.of(query.getCorpus().get(0), 0.9));
        AskDataRetriever retriever = AskDataRetriever.forStrategy(
                query -> Collections.singletonList(TableSchema.builder().name("users").build()),
                IndexMode.ECONOMICAL, vector, lexical, null);

        List<Retrieved<Document>> result = retriever.retrieve(
                RetrievalQuery.builder().text("users").build());

        assertEquals(1, result.size());
        assertTrue(!vectorCalled.get());
    }

    @Test
    void highQualityStrategyUsesHybridRetrieval() {
        AtomicBoolean vectorCalled = new AtomicBoolean();
        AtomicBoolean lexicalCalled = new AtomicBoolean();
        Retriever<Document> vector = query -> {
            vectorCalled.set(true);
            return Collections.singletonList(Retrieved.of(query.getCorpus().get(0), 0.9));
        };
        Retriever<Document> lexical = query -> {
            lexicalCalled.set(true);
            return Collections.singletonList(Retrieved.of(query.getCorpus().get(0), 0.8));
        };
        AskDataRetriever retriever = AskDataRetriever.forStrategy(
                query -> Collections.singletonList(TableSchema.builder().name("users").build()),
                IndexMode.HIGH_QUALITY, vector, lexical, null);

        List<Retrieved<Document>> result = retriever.retrieve(
                RetrievalQuery.builder().text("users").build());

        assertEquals(1, result.size());
        assertTrue(vectorCalled.get());
        assertTrue(lexicalCalled.get());
    }
}
