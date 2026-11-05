package com.xiongx9527.fw.plus.ai.langchain4j.adapter.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.LangChain4jStorePort;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class LangChain4jContentRetrieverAdapterTest {

    private static ContentRetriever stub() {
        return new ContentRetriever() {
            @Override
            public List<Content> retrieve(Query query) {
                List<Content> out = new ArrayList<Content>();
                for (int i = 1; i <= 3; i++) {
                    Metadata metadata = new Metadata();
                    metadata.put(LangChain4jStorePort.KEY_ID, String.valueOf(i));
                    out.add(Content.from(TextSegment.from("doc-" + i, metadata)));
                }
                return out;
            }
        };
    }

    @Test
    void convertsContentToDocuments() {
        LangChain4jContentRetrieverAdapter adapter =
                new LangChain4jContentRetrieverAdapter(stub());

        List<Retrieved<Document>> hits = adapter.retrieve(RetrievalQuery.builder().text("q").build());

        assertEquals(3, hits.size());
        assertEquals("1", hits.get(0).getItem().getId());
        assertEquals("doc-1", hits.get(0).getItem().getContent());
        assertEquals(1.0, hits.get(0).getScore(), 0.0001);
        assertEquals("langchain4j", hits.get(0).getSource());
    }

    @Test
    void topNTruncatesAndThresholdFilters() {
        LangChain4jContentRetrieverAdapter adapter =
                new LangChain4jContentRetrieverAdapter(stub(), 0.4, "rag");

        assertEquals(2, adapter.retrieve(RetrievalQuery.builder().text("q").topN(2).build()).size());
        assertTrue(adapter.retrieve(RetrievalQuery.builder()
                .text("q").scoreThreshold(0.5).build()).isEmpty());
    }

    @Test
    void blankQuerySkipsUpstream() {
        LangChain4jContentRetrieverAdapter adapter =
                new LangChain4jContentRetrieverAdapter(stub());
        assertTrue(adapter.retrieve(RetrievalQuery.builder().text("  ").build()).isEmpty());
        assertTrue(adapter.retrieve(null).isEmpty());
    }

    @Test
    void emptyUpstreamYieldsEmptyResult() {
        LangChain4jContentRetrieverAdapter adapter = new LangChain4jContentRetrieverAdapter(
                query -> Collections.<Content>emptyList());
        assertTrue(adapter.retrieve(RetrievalQuery.builder().text("q").build()).isEmpty());
    }
}
