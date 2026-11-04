package com.xiongx9527.fw.plus.ai.trace.exporter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Iso8601Test {
    @Test
    void formatsEpochAsInstant() {
        String s = Iso8601.fromEpochMillis(1700000000000L);
        assertNotNull(s);
        assertTrue(s.contains("T"));
        assertTrue(s.endsWith("Z"));
    }
}
