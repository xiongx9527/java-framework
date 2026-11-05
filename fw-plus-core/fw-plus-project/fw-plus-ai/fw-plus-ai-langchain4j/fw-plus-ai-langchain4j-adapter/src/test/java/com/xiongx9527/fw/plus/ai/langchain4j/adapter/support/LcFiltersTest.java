package com.xiongx9527.fw.plus.ai.langchain4j.adapter.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.Test;

class LcFiltersTest {

    @Test
    void noOpIsNotPushed() {
        assertNull(LcFilters.toUpstream(null));
        assertNull(LcFilters.toUpstream(Filter.NO_OP));
    }

    @Test
    void equalConditionCanBePushed() {
        Filter filter = Filter.builder().eq("dsId", 7).build();
        dev.langchain4j.store.embedding.filter.Filter pushed = LcFilters.toUpstream(filter);
        assertNotNull(pushed);
        Metadata metadata = new Metadata();
        metadata.put("dsId", 7);
        assertTrue(pushed.test(metadata));
    }

    @Test
    void compositeConditionCombinesChildren() {
        Filter filter = Filter.builder().eq("dsId", 7).gt("score", 0.5).build();
        dev.langchain4j.store.embedding.filter.Filter pushed = LcFilters.toUpstream(filter);
        assertNotNull(pushed);
        Metadata metadata = new Metadata();
        metadata.put("dsId", 7);
        metadata.put("score", 0.8);
        assertTrue(pushed.test(metadata));
    }

    @Test
    void unsupportedConditionAbortsWholePushdown() {
        assertNull(LcFilters.toUpstream(Filter.builder().contains("tag", "x").build()));
        assertNull(LcFilters.toUpstream(
                Filter.builder().eq("dsId", 7).contains("tag", "x").build()));
    }
}
