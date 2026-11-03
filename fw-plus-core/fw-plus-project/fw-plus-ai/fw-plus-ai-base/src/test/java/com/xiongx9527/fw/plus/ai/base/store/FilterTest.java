package com.xiongx9527.fw.plus.ai.base.store;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilterTest {

    @Test
    public void eqAndRange() {
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("dsId", 7);
        meta.put("score", 0.8);
        Filter f = Filter.builder().eq("dsId", 7).gt("score", 0.5).build();
        assertTrue(f.matches(meta));
        assertFalse(Filter.builder().eq("dsId", 8).build().matches(meta));
        assertFalse(Filter.builder().lt("score", 0.5).build().matches(meta));
    }

    @Test
    public void inAndContains() {
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("type", "report");
        meta.put("tag", "finance-quarterly");
        assertTrue(Filter.builder().in("type", Arrays.asList("report", "ad-hoc")).build().matches(meta));
        assertTrue(Filter.builder().contains("tag", "quarterly").build().matches(meta));
        assertFalse(Filter.builder().contains("tag", "weekly").build().matches(meta));
    }

    @Test
    public void orAndNoOp() {
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("a", 1);
        assertTrue(Filter.builder().or(Filter.builder().eq("a", 1).build(),
                Filter.builder().eq("a", 2).build()).build().matches(meta));
        assertTrue(Filter.NO_OP.matches(meta));
    }
}
