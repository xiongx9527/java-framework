package com.xiongx9527.fw.plus.ai.base.store.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 锁定 {@link Filter} 的只读结构：adapter 可据此把条件树下推到后端原生 filter。 */
class FilterStructureTest {

    @Test
    void noOpIsDetectable() {
        assertTrue(Filter.NO_OP.isNoOp());
        assertEquals(Filter.Op.AND, Filter.NO_OP.op());
        assertTrue(Filter.NO_OP.children().isEmpty());
        assertNull(Filter.NO_OP.key());
        assertNull(Filter.NO_OP.value());
    }

    @Test
    void leafConditionExposesStructure() {
        Filter f = Filter.builder().eq("dsId", 7).build();
        assertEquals(Filter.Op.EQ, f.op());
        assertEquals("dsId", f.key());
        assertEquals(Integer.valueOf(7), f.value());
        assertTrue(f.children().isEmpty());
        assertFalse(f.isNoOp());
    }

    @Test
    void compositeConditionExposesChildren() {
        Filter f = Filter.builder().eq("dsId", 7).gt("score", 0.5).build();
        assertEquals(Filter.Op.AND, f.op());
        assertEquals(2, f.children().size());
        assertEquals(Filter.Op.EQ, f.children().get(0).op());
        assertEquals(Filter.Op.GT, f.children().get(1).op());
        assertEquals("score", f.children().get(1).key());
        assertEquals(Double.valueOf(0.5), f.children().get(1).value());
    }

    @Test
    void explicitOrIsPreserved() {
        Filter f = Filter.builder()
                .or(Filter.builder().eq("a", 1).build(), Filter.builder().eq("a", 2).build())
                .build();
        assertEquals(Filter.Op.OR, f.op());
        assertEquals(2, f.children().size());
    }

    @Test
    void childrenAreImmutable() {
        Filter f = Filter.builder().eq("a", 1).gt("b", 2).build();
        assertThrows(UnsupportedOperationException.class,
                () -> f.children().add(Filter.builder().eq("c", 3).build()));
    }

    @Test
    void matchesStillAgreesWithStructure() {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("dsId", 7);
        meta.put("score", 0.8);
        Filter f = Filter.builder().eq("dsId", 7).gt("score", 0.5).build();
        assertTrue(f.matches(meta));
        assertFalse(Filter.builder().eq("dsId", 8).build().matches(meta));
    }
}
