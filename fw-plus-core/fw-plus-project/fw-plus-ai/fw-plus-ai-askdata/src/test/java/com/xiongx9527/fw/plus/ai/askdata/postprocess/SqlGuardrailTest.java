package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 通用 SQL 确定性护栏单测（Java 8 + JSqlParser 4.6）。 */
class SqlGuardrailTest {

    private static Map<String, Map<String, String>> schema() {
        Map<String, String> account = new LinkedHashMap<String, String>();
        account.put("id", "int");
        account.put("name", "varchar");
        account.put("created_at", "timestamp");
        Map<String, Map<String, String>> schema = new LinkedHashMap<String, Map<String, String>>();
        schema.put("account", account);
        return schema;
    }

    @Test
    void repairsQualifiedMissingColumn() {
        String sql = "SELECT name FROM account WHERE nam = 'x'";
        String error = "SQL 引用了表 account 中不存在的列：nam";
        String fixed = SqlGuardrail.repair(sql, error, schema());
        assertNotNull(fixed);
        assertTrue(fixed.toLowerCase().contains("name"), fixed);
        assertFalse(fixed.toLowerCase().contains("nam ="), fixed);
    }

    @Test
    void repairsBareMissingColumn() {
        String sql = "SELECT nam FROM account";
        String error = "SQL 引用了不存在的列：nam";
        String fixed = SqlGuardrail.repair(sql, error, schema());
        assertNotNull(fixed);
        assertTrue(fixed.toLowerCase().contains("name"), fixed);
    }

    @Test
    void appendsGroupByWhenMissing() {
        String sql = "SELECT name, COUNT(*) FROM account";
        String error = "聚合查询缺少 GROUP BY 维度：name";
        String fixed = SqlGuardrail.repair(sql, error, schema());
        assertNotNull(fixed);
        assertTrue(fixed.toUpperCase().contains("GROUP BY NAME"), fixed);
    }

    @Test
    void appendsToExistingGroupBy() {
        String sql = "SELECT id, name FROM account GROUP BY id ORDER BY id";
        String error = "聚合查询缺少 GROUP BY 维度：name";
        String fixed = SqlGuardrail.repair(sql, error, schema());
        assertNotNull(fixed);
        assertTrue(fixed.toUpperCase().contains("GROUP BY ID, NAME"), fixed);
        assertTrue(fixed.toUpperCase().contains("ORDER BY ID"), fixed);
    }

    @Test
    void insertsGroupByBeforeOrderAndLimit() {
        String sql = "SELECT name, COUNT(*) FROM account ORDER BY name LIMIT 10";
        String error = "聚合查询缺少 GROUP BY 维度：name";
        String fixed = SqlGuardrail.repairMissingGroupBy(sql, error);
        assertNotNull(fixed);
        assertTrue(fixed.toUpperCase().indexOf("GROUP BY") < fixed.toUpperCase().indexOf("ORDER BY"), fixed);
        assertTrue(fixed.toUpperCase().contains("LIMIT 10"), fixed);
    }

    @Test
    void returnsNullWhenNoSafeRepair() {
        assertNull(SqlGuardrail.repair("SELECT * FROM account", "未知错误", schema()));
        assertNull(SqlGuardrail.repair("SELECT * FROM account", "SQL 引用了表 account 中不存在的列：xyz_no_such_col_long", schema()));
        assertNull(SqlGuardrail.repair(null, "x", schema()));
        assertNull(SqlGuardrail.repair("SELECT 1", null, schema()));
    }

    @Test
    void levenshteinAndClosestAreCorrect() {
        assertEquals(1, SqlGuardrail.levenshtein("nam", "name"));
        assertEquals(3, SqlGuardrail.levenshtein("kitten", "sitting"));
        assertEquals("created_at", SqlGuardrail.closest("created_ad", schema().get("account").keySet()));
        assertNull(SqlGuardrail.closest("totally_different_long_column", schema().get("account").keySet()));
    }

    @Test
    void replaceColumnDoesNotTouchStringLiteralOrOtherTable() {
        String sql = "SELECT a.nam, 'nam' AS literal FROM account a";
        String fixed = SqlGuardrail.replaceColumn(sql, "a", "nam", "name");
        assertNotNull(fixed);
        assertTrue(fixed.contains("a.name"), fixed);
        assertTrue(fixed.contains("'nam'"), fixed);
    }
}
