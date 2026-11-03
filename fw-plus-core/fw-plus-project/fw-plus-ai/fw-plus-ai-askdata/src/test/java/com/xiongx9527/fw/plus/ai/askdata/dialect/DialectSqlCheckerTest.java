package com.xiongx9527.fw.plus.ai.askdata.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class DialectSqlCheckerTest {

    @Test
    void flagsBacktickInDoubleQuoteDialect() {
        List<String> issues = DialectSqlChecker.check("SELECT `id` FROM orders", SqlDialect.POSTGRESQL);
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).contains("backtick"));
    }

    @Test
    void ignoresBacktickInBacktickDialect() {
        List<String> issues = DialectSqlChecker.check("SELECT `id` FROM orders", SqlDialect.MYSQL);
        assertTrue(issues.isEmpty());
    }

    @Test
    void flagsLimitInOracle() {
        List<String> issues = DialectSqlChecker.check("SELECT id FROM orders LIMIT 10", SqlDialect.ORACLE);
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).contains("does not support LIMIT"));
    }

    @Test
    void ignoresLimitInMysql() {
        List<String> issues = DialectSqlChecker.check("SELECT id FROM orders LIMIT 10", SqlDialect.MYSQL);
        assertTrue(issues.isEmpty());
    }

    @Test
    void flagsTopInPostgresql() {
        List<String> issues = DialectSqlChecker.check("SELECT TOP 10 id FROM orders", SqlDialect.POSTGRESQL);
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).contains("does not support TOP"));
    }

    @Test
    void ignoresTopInSqlServer() {
        List<String> issues = DialectSqlChecker.check("SELECT TOP 10 id FROM orders", SqlDialect.SQLSERVER);
        assertTrue(issues.isEmpty());
    }

    @Test
    void returnsEmptyForUnknownDialect() {
        List<String> issues = DialectSqlChecker.check("SELECT `id` FROM orders LIMIT 10", SqlDialect.UNKNOWN);
        assertTrue(issues.isEmpty());
    }

    @Test
    void helperMethodsConsistent() {
        assertTrue(DialectSqlChecker.usesDoubleQuote(SqlDialect.POSTGRESQL));
        assertFalse(DialectSqlChecker.usesDoubleQuote(SqlDialect.MYSQL));
        assertTrue(DialectSqlChecker.supportsLimit(SqlDialect.MYSQL));
        assertFalse(DialectSqlChecker.supportsLimit(SqlDialect.ORACLE));
        assertEquals(2, DialectSqlChecker.check("SELECT `id` FROM t LIMIT 5", SqlDialect.ORACLE).size());
    }
}
