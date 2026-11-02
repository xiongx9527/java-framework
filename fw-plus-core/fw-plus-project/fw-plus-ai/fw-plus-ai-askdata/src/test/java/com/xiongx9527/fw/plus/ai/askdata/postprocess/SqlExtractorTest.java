package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationException;
import org.junit.jupiter.api.Test;

public class SqlExtractorTest {

    private static final String F = String.valueOf('\u0060') + String.valueOf('\u0060') + String.valueOf('\u0060');

    @Test
    void extractsFencedSql() {
        String out = "说明\n" + F + "sql\nSELECT id FROM t WHERE a = 1\n" + F;
        assertEquals("SELECT id FROM t WHERE a = 1", SqlExtractor.extract(out));
    }

    @Test
    void extractsJsonSql() {
        String out = "{\"sql\":\"SELECT 1\"}";
        assertEquals("SELECT 1", SqlExtractor.extract(out));
    }

    @Test
    void extractsPlainSql() {
        String out = "SELECT name FROM users";
        assertEquals("SELECT name FROM users", SqlExtractor.extract(out));
    }

    @Test
    void stripsTrailingSemicolon() {
        String out = "SELECT 1;";
        assertEquals("SELECT 1", SqlExtractor.extract(out));
    }

    @Test
    void throwsWhenEmpty() {
        assertThrows(SqlGenerationException.class, () -> SqlExtractor.extract("   "));
    }

    @Test
    void extractExplanationReturnsTextBeforeFence() {
        String out = "这是说明\n" + F + "sql\nSELECT 1\n" + F;
        assertEquals("这是说明", SqlExtractor.extractExplanation(out));
    }
}
