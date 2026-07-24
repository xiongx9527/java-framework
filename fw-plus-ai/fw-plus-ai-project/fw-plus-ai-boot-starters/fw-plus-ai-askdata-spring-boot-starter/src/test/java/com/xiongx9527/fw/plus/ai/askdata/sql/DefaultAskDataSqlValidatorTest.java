package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialectRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultAskDataSqlValidatorTest {

    private final DefaultAskDataSqlValidator validator = new DefaultAskDataSqlValidator();
    private final AskDataDialect mysql = new AskDataDialectRegistry().getDialect("mysql");

    @Test
    public void shouldRejectEmptySql() {
        assertRejected("   ");
    }

    @Test
    public void shouldRejectMultipleStatements() {
        assertRejected("select * from orders; select * from users");
    }

    @Test
    public void shouldRejectStatementHiddenAfterComment() {
        assertRejected("select * from orders; /* generated comment */ delete from orders");
    }

    @Test
    public void shouldRejectDangerousKeywordsOutsideStringLiterals() {
        String[] dangerousSql = {
                "insert into orders values (1)",
                "update orders set amount = 1",
                "delete from orders",
                "drop table orders",
                "alter table orders add flag int",
                "truncate table orders",
                "create table orders_copy(id bigint)",
                "merge into orders key(id) values (1)"
        };

        for (String sql : dangerousSql) {
            assertRejected(sql);
        }
    }

    @Test
    public void shouldRejectWritableSelectInto() {
        assertRejected("select * into audit_copy from orders");
    }

    @Test
    public void shouldRejectMysqlExecutableComment() {
        assertRejected("select 1 limit 1 /*!50000; delete from orders */");
    }

    @Test
    public void shouldAllowReadOnlySelectAndKeywordsInStringLiterals() {
        AskDataSqlValidationResult result = validator.validate("select 'drop table orders' as note", mysql, 100);

        assertTrue(result.isValid());
        assertEquals("select 'drop table orders' as note limit 100", result.getSql());
    }

    @Test
    public void shouldAllowReadOnlyWithQuery() {
        AskDataSqlValidationResult result = validator.validate(
                "with order_total as (select count(*) as total from orders) select total from order_total", mysql, 100);

        assertTrue(result.isValid());
        assertEquals("with order_total as (select count(*) as total from orders) select total from order_total limit 100",
                result.getSql());
    }

    @Test
    public void shouldKeepExistingLimit() {
        AskDataSqlValidationResult result = validator.validate("select * from orders limit 10", mysql, 100);

        assertTrue(result.isValid());
        assertEquals("select * from orders limit 10", result.getSql());
    }

    @Test
    public void shouldAddLimitWhenMissing() {
        AskDataSqlValidationResult result = validator.validate("select * from orders", mysql, 100);

        assertTrue(result.isValid());
        assertEquals("select * from orders limit 100", result.getSql());
    }

    @Test
    public void shouldAddLimitThroughDialect() {
        AskDataDialect fetchDialect = new AskDataDialect("fetch", "", "") {
            @Override
            public String applyLimit(String sql, int maxRows) {
                return sql + " fetch first " + maxRows + " rows only";
            }
        };

        AskDataSqlValidationResult result = validator.validate("select * from orders", fetchDialect, 100);

        assertTrue(result.isValid());
        assertEquals("select * from orders fetch first 100 rows only", result.getSql());
    }

    @Test
    public void shouldAddLimitBeforeTrailingCommentAfterSemicolon() {
        AskDataSqlValidationResult result = validator.validate("select * from orders; /* generated SQL */", mysql, 100);

        assertTrue(result.isValid());
        assertEquals("select * from orders limit 100", result.getSql());
    }

    @Test
    public void shouldAddLimitBeforeTrailingLineComment() {
        AskDataSqlValidationResult result = validator.validate("select * from orders -- generated", mysql, 100);

        assertTrue(result.isValid());
        assertEquals("select * from orders limit 100 -- generated", result.getSql());
    }

    private void assertRejected(String sql) {
        AskDataSqlValidationResult result = validator.validate(sql, mysql, 100);
        assertFalse(result.isValid());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
    }
}
