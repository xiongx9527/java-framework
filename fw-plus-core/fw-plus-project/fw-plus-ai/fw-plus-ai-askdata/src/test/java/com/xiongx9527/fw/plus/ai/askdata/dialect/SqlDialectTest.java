package com.xiongx9527.fw.plus.ai.askdata.dialect;
import com.xiongx9527.fw.plus.jdbc.dialect.DialectHelper;
import com.xiongx9527.fw.plus.jdbc.dialect.SqlDialect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SqlDialectTest {

    @Test
    void fromHintRecognizesCommonDialects() {
        assertEquals(SqlDialect.MYSQL, SqlDialect.fromHint("mysql"));
        assertEquals(SqlDialect.POSTGRESQL, SqlDialect.fromHint("postgresql"));
        assertEquals(SqlDialect.POSTGRESQL, SqlDialect.fromHint("pg"));
        assertEquals(SqlDialect.CLICKHOUSE, SqlDialect.fromHint("ck"));
        assertEquals(SqlDialect.SQLSERVER, SqlDialect.fromHint("sqlServer"));
        assertEquals(SqlDialect.ORACLE, SqlDialect.fromHint("oracle"));
        assertEquals(SqlDialect.DM, SqlDialect.fromHint("dm"));
        assertEquals(SqlDialect.REDSHIFT, SqlDialect.fromHint("redshift"));
        assertEquals(SqlDialect.KINGBASE, SqlDialect.fromHint("kingbase"));
        assertEquals(SqlDialect.EXCEL, SqlDialect.fromHint("excel"));
    }

    @Test
    void fromHintReturnsUnknownForNullAndGarbage() {
        assertEquals(SqlDialect.UNKNOWN, SqlDialect.fromHint(null));
        assertEquals(SqlDialect.UNKNOWN, SqlDialect.fromHint("   "));
        assertEquals(SqlDialect.UNKNOWN, SqlDialect.fromHint("made-up-db"));
    }

    @Test
    void quoteUsesBacktickForMysql() {
        String q = DialectHelper.quote(SqlDialect.MYSQL, "col");
        assertEquals(String.valueOf('\u0060') + "col" + String.valueOf('\u0060'), q);
    }

    @Test
    void quoteUsesDoubleQuoteForPostgres() {
        String q = DialectHelper.quote(SqlDialect.POSTGRESQL, "col");
        assertEquals("\"col\"", q);
    }
}
