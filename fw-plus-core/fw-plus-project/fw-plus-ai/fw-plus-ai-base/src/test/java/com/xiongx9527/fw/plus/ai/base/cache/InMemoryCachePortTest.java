package com.xiongx9527.fw.plus.ai.base.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InMemoryCachePortTest {

    @Test
    void storesAndExpires() throws Exception {
        InMemoryCachePort cache = new InMemoryCachePort();
        cache.put("k", "v", 30);

        assertEquals("v", cache.get("k"));
        Thread.sleep(60);
        assertNull(cache.get("k"));
    }

    @Test
    void bumpEpochClearsEverything() {
        InMemoryCachePort cache = new InMemoryCachePort();
        cache.put("k", "v", 0);

        long next = cache.bumpEpoch();

        assertTrue(next > 0);
        assertEquals(next, cache.epoch());
        assertNull(cache.get("k"));
    }
}
