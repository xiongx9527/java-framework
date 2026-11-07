package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationException;
import com.xiongx9527.fw.plus.ai.askdata.chart.ChartType;
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

    @Test
    void extractsBriefFromStructuredJson() {
        String out = "{\"sql\":\"SELECT 1\",\"brief\":\"用户订单概览\",\"chart-type\":\"bar\"}";
        assertEquals("用户订单概览", SqlExtractor.extractBrief(out));
    }

    @Test
    void extractsChartTypeFromStructuredJson() {
        String out = "{\"sql\":\"SELECT 1\",\"brief\":\"x\",\"chart-type\":\"pie\"}";
        assertEquals(ChartType.PIE, SqlExtractor.extractChartType(out));
    }

    @Test
    void extractsChartTypeAcceptsCamelCaseKey() {
        String out = "{\"chartType\":\"line\",\"sql\":\"SELECT 1\"}";
        assertEquals(ChartType.LINE, SqlExtractor.extractChartType(out));
    }

    @Test
    void extractsSqlFromFencedJson() {
        String out = "```json\n{\"sql\":\"SELECT 1\",\"brief\":\"x\",\"chart-type\":\"bar\"}\n```";
        assertEquals("SELECT 1", SqlExtractor.extract(out));
    }

    @Test
    void extractsSqlAndMetadataFromJsonObject() {
        String out = "{\"sql\":\"SELECT name FROM users\",\"brief\":\"用户列表\",\"chart-type\":\"pie\"}";
        assertEquals("SELECT name FROM users", SqlExtractor.extract(out));
        assertEquals("用户列表", SqlExtractor.extractBrief(out));
        assertEquals(ChartType.PIE, SqlExtractor.extractChartType(out));
    }

    @Test
    void extractsBriefAndChartTypeReturnNullWhenAbsent() {
        String out = "SELECT name FROM users";
        assertNull(SqlExtractor.extractBrief(out));
        assertNull(SqlExtractor.extractChartType(out));
    }
}
