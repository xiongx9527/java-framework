package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FilterExpressionsPushdownTest {

    // -------------------------------------------------------------- Qdrant

    @Test
    void translatesQdrantEqLeaf() {
        Map<String, Object> m = FilterExpressions.toQdrant(Filter.builder().eq("dsId", 7).build());
        assertEquals("dsId", m.get("key"));
        @SuppressWarnings("unchecked")
        Map<String, Object> match = (Map<String, Object>) m.get("match");
        assertEquals(7, match.get("value"));
    }

    @Test
    void translatesQdrantAndToMust() {
        Map<String, Object> m = FilterExpressions.toQdrant(
                Filter.builder().eq("dsId", 7).gt("score", 0.5).build());
        assertTrue(m.containsKey("must"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) m.get("must");
        assertEquals(2, parts.size());
    }

    @Test
    void qdrantContainsUnsupported() {
        assertNull(FilterExpressions.toQdrant(Filter.builder().contains("tag", "x").build()));
    }

    // ------------------------------------------------------------- PgVector

    @Test
    void translatesPgVectorEqToJsonbContainment() {
        String sql = FilterExpressions.toPgVector(Filter.builder().eq("dsId", 7).build());
        assertTrue(sql.contains("metadata @>"), sql);
        assertTrue(sql.contains("dsId"), sql);
    }

    @Test
    void translatesPgVectorRange() {
        String sql = FilterExpressions.toPgVector(Filter.builder().gt("score", 0.5).build());
        assertTrue(sql.contains("::numeric > 0.5"), sql);
    }

    @Test
    void translatesPgVectorContainsToLike() {
        String sql = FilterExpressions.toPgVector(Filter.builder().contains("tag", "x").build());
        assertTrue(sql.contains("LIKE '%x%'"), sql);
    }

    // ------------------------------------------------------------- Weaviate

    @Test
    void translatesWeaviateEqToWhereNode() {
        Map<String, Object> w = FilterExpressions.toWeaviate(Filter.builder().eq("dsId", 7).build());
        assertEquals("Equal", w.get("operator"));
        @SuppressWarnings("unchecked")
        List<String> path = (List<String>) w.get("path");
        assertEquals("dsId", path.get(0));
        assertEquals(7, w.get("valueNumber"));
    }

    @Test
    void translatesWeaviateStringToValueString() {
        Map<String, Object> w = FilterExpressions.toWeaviate(Filter.builder().eq("name", "a").build());
        assertEquals("valueString", "valueString");
        assertEquals("a", w.get("valueString"));
    }

    @Test
    void translatesWeaviateContainsToLike() {
        Map<String, Object> w = FilterExpressions.toWeaviate(Filter.builder().contains("tag", "x").build());
        assertEquals("Like", w.get("operator"));
        assertEquals("*x*", w.get("valueText"));
    }

    // ---------------------------------------------------------------- Redis

    @Test
    void translatesRedisEqToFieldExpr() {
        String expr = FilterExpressions.toRedis(Filter.builder().eq("dsId", 7).build());
        assertTrue(expr.contains("@m_dsId:"), expr);
    }

    @Test
    void translatesRedisInToPipeList() {
        String expr = FilterExpressions.toRedis(
                Filter.builder().in("dsId", java.util.Arrays.asList(1, 2)).build());
        assertTrue(expr.contains("@m_dsId:("), expr);
        assertTrue(expr.contains("|"), expr);
    }

    @Test
    void redisRangeUnsupportedReturnsNull() {
        assertNull(FilterExpressions.toRedis(Filter.builder().gt("score", 0.5).build()));
    }

    @Test
    void translatesRedisContainsToWildcard() {
        String expr = FilterExpressions.toRedis(Filter.builder().contains("tag", "x").build());
        assertTrue(expr.contains("@m_tag:(*x*)"), expr);
    }

    // ------------------------------------------------------------- NO_OP

    @Test
    void noOpReturnsNullForAll() {
        assertNull(FilterExpressions.toQdrant(Filter.NO_OP));
        assertNull(FilterExpressions.toPgVector(Filter.NO_OP));
        assertNull(FilterExpressions.toWeaviate(Filter.NO_OP));
        assertNull(FilterExpressions.toRedis(Filter.NO_OP));
    }
}
