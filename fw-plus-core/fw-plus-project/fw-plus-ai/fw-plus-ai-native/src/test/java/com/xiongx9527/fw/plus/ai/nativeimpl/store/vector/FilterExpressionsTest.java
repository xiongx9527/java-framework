package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FilterExpressionsTest {

    @Test
    void translatesCompositeToMilvusExpression() {
        Filter filter = Filter.builder().eq("dsId", 7).gt("score", 0.5).build();
        assertEquals("(dsId == 7 and score > 0.5)", FilterExpressions.toMilvus(filter));
    }

    @Test
    void unsupportedContainsReturnsNull() {
        assertNull(FilterExpressions.toMilvus(Filter.builder().contains("tag", "x").build()));
        assertNull(FilterExpressions.toMilvus(
                Filter.builder().eq("dsId", 7).contains("tag", "x").build()));
        assertNull(FilterExpressions.toMilvus(Filter.NO_OP));
    }

    @Test
    void translatesToElasticsearchDsl() {
        Filter filter = Filter.builder().eq("dsId", 7).build();

        @SuppressWarnings("unchecked")
        Map<String, Object> term = (Map<String, Object>) FilterExpressions.toElasticsearch(filter).get("term");
        assertEquals(7, term.get("dsId"));
    }
}
