package com.xiongx9527.fw.plus.ai.base.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class WebRetrieverTest {

    /** 具名 web 检索器：实现 {@link WebRetriever} 即为 {@link DocumentRetriever}。 */
    private static final class FakeWebRetriever implements WebRetriever {
        @Override
        public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
            return Collections.singletonList(Retrieved.of(Document.of("web1", "web"), 0.8));
        }
    }

    @Test
    void webRetrieverIsDocumentRetriever() {
        WebRetriever web = new FakeWebRetriever();
        DocumentRetriever asDocumentRetriever = web;
        assertTrue(asDocumentRetriever instanceof Retriever,
                "WebRetriever must be a DocumentRetriever / Retriever<Document>");
    }

    @Test
    void delegatesRetrievalThroughSeam() {
        WebRetriever web = new FakeWebRetriever();
        List<Retrieved<Document>> hits = web.retrieve(RetrievalQuery.builder().text("q").build());
        assertEquals(1, hits.size());
        assertEquals("web1", hits.get(0).getItem().getId());
    }
}
