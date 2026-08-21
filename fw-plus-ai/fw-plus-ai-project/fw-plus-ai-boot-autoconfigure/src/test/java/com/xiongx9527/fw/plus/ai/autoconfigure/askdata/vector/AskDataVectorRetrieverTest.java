package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AskDataVectorRetrieverTest {

    @Test
    public void shouldReturnEmptyListWhenNoRetrieverIsAvailable() {
        AskDataVectorRetriever retriever = new CompositeAskDataVectorRetriever(
                Collections.<AskDataVectorRetriever>emptyList());
        AskDataVectorQuery query = new AskDataVectorQuery("schema", "order", "vec", "订单数", 3,
                Collections.<String, Object>emptyMap());

        assertTrue(retriever.retrieve(query).isEmpty());
    }

    @Test
    public void shouldAdaptContentRetrieverDocuments() {
        ContentRetriever contentRetriever = new ContentRetriever() {
            @Override
            public List<Content> retrieve(Query query) {
                return Arrays.asList(Content.from(TextSegment.from("术语: GMV = amount 求和")));
            }
        };
        AskDataVectorRetriever retriever = new ContentRetrieverAskDataVectorRetriever(contentRetriever);

        List<AskDataRetrievedDocument> documents = retriever.retrieve(new AskDataVectorQuery("term", "order",
                "vec", "GMV", 3, Collections.<String, Object>emptyMap()));

        assertEquals(1, documents.size());
        assertEquals("术语: GMV = amount 求和", documents.get(0).getContent());
    }

    @Test
    public void shouldUseConfiguredDefaultTopKWhenQueryDoesNotSpecifyOne() {
        final int[] actualTopK = new int[1];
        AskDataVectorRetriever delegate = new AskDataVectorRetriever() {
            @Override
            public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                actualTopK[0] = query.getTopK();
                return Collections.emptyList();
            }
        };
        AskDataVectorRetriever retriever = new CompositeAskDataVectorRetriever(
                Collections.singletonList(delegate), 7);

        retriever.retrieve(new AskDataVectorQuery("schema", "order", "vec", "订单数", 0,
                Collections.<String, Object>emptyMap()));

        assertEquals(7, actualTopK[0]);
    }

    @Test
    public void shouldDeeplyProtectAuthorizationFilters() {
        List<String> tables = new java.util.ArrayList<String>(Collections.singletonList("orders"));
        Map<String, Object> scope = new LinkedHashMap<String, Object>();
        scope.put("tables", tables);
        Map<String, Object> filters = new LinkedHashMap<String, Object>();
        filters.put("scope", scope);

        AskDataVectorQuery query = new AskDataVectorQuery("schema", "order", "vec", "订单数", 3, filters);
        tables.add("secrets");

        assertEquals(Collections.singletonList("orders"),
                ((Map<?, ?>) query.getFilters().get("scope")).get("tables"));
        assertThrows(UnsupportedOperationException.class,
                () -> ((List<Object>) ((Map<?, ?>) query.getFilters().get("scope")).get("tables"))
                        .add("secrets"));
    }
}
