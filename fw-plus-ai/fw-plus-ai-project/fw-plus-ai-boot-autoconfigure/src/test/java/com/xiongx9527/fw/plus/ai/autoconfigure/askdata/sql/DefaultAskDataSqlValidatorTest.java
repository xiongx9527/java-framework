package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataColumn;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void shouldRejectSideEffectFunctions() {
        assertRejected("select setval('order_seq', 10)");
        assertRejected("select nextval('order_seq')");
        assertRejected("select \"nextval\"('order_seq')");
        assertRejected("select pg_catalog.\"setval\"('order_seq', 10)");
        assertRejected("select pg_notify('events', 'order created')");
        assertRejected("select lo_unlink(42)");
    }

    @Test
    public void shouldRejectLockingQueries() {
        assertRejected("select * from orders for update");
        assertRejected("select * from orders for share");
        assertRejected("select * from orders lock in share mode");
    }

    @Test
    public void shouldRejectTablesOutsideMetadataScope() throws Exception {
        AskDataMetadata metadata = new AskDataMetadata("mysql",
                Collections.singletonList(tableAuthorizedAtTableLevel("orders")),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Method method = DefaultAskDataSqlValidator.class.getMethod("validate", String.class, AskDataDialect.class,
                Integer.TYPE, AskDataMetadata.class);

        AskDataSqlValidationResult allowed = (AskDataSqlValidationResult) method.invoke(validator,
                "select * from orders", mysql, Integer.valueOf(100), metadata);
        AskDataSqlValidationResult rejected = (AskDataSqlValidationResult) method.invoke(validator,
                "select * from users", mysql, Integer.valueOf(100), metadata);

        assertTrue(allowed.isValid());
        assertFalse(rejected.isValid());
        assertEquals(AskDataErrorCode.SQL_REJECTED, rejected.getErrorCode());
    }

    @Test
    public void shouldRejectUnauthorizedSelectedColumnAgainstMetadataScope() {
        AskDataMetadata metadata = new AskDataMetadata("mysql", Collections.singletonList(
                new AskDataTable("orders", Collections.singletonList(new AskDataColumn("id", "bigint")))),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        AskDataSqlValidationResult result = validator.validate("select secret from orders", mysql, 100, metadata);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("未授权列"));
    }

    @Test
    public void shouldRejectColumnWhenAuthorizedMetadataHasNoExplicitColumns() {
        AskDataMetadata metadata = new AskDataMetadata("mysql", Collections.singletonList(
                new AskDataTable("orders", Collections.<AskDataColumn>emptyList())),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        AskDataSqlValidationResult result = validator.validate("select id from orders", mysql, 100, metadata);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("未授权列"));
    }

    @Test
    public void shouldAllowColumnOnlyForExplicitTableLevelAuthorization() {
        AskDataMetadata metadata = new AskDataMetadata("mysql",
                Collections.singletonList(tableAuthorizedAtTableLevel("orders")),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        AskDataSqlValidationResult result = validator.validate("select id from orders", mysql, 100, metadata);

        assertTrue(result.isValid());
    }

    @Test
    public void shouldRejectUnauthorizedFilterColumnAgainstMetadataScope() {
        AskDataMetadata metadata = new AskDataMetadata("mysql", Collections.singletonList(
                new AskDataTable("orders", Collections.singletonList(new AskDataColumn("id", "bigint")))),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        AskDataSqlValidationResult result = validator.validate(
                "select id from orders where secret = 'hidden'", mysql, 100, metadata);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("未授权列"));
    }

    @Test
    public void shouldAllowAuthorizedColumnInsideFunctionAndAlias() {
        AskDataMetadata metadata = new AskDataMetadata("mysql", Collections.singletonList(
                new AskDataTable("orders", Collections.singletonList(new AskDataColumn("id", "bigint")))),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        AskDataSqlValidationResult result = validator.validate(
                "select max(o.id) as max_id from orders o order by max_id", mysql, 100, metadata);

        assertTrue(result.isValid());
    }

    @Test
    public void shouldRejectRealColumnHiddenBySelectAliasInSameScope() {
        AskDataMetadata metadata = new AskDataMetadata("mysql", Collections.singletonList(
                new AskDataTable("orders", Collections.singletonList(new AskDataColumn("id", "bigint")))),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        AskDataSqlValidationResult result = validator.validate(
                "select id as secret, secret from orders", mysql, 100, metadata);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("未授权列"));
    }

    @Test
    public void shouldRejectUnqualifiedColumnWithoutUniqueAuthorizedTable() {
        AskDataMetadata metadata = new AskDataMetadata("mysql", Arrays.asList(
                new AskDataTable("orders", Collections.singletonList(new AskDataColumn("id", "bigint"))),
                new AskDataTable("customers", Collections.singletonList(new AskDataColumn("name", "varchar")))),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        AskDataSqlValidationResult result = validator.validate(
                "select id from orders join customers on orders.id = customers.name", mysql, 100, metadata);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("未授权列"));
    }

    @Test
    public void shouldRejectBaseTableAliasThatCollidesWithCteName() {
        AskDataMetadata metadata = new AskDataMetadata("mysql", Collections.singletonList(
                new AskDataTable("orders", Collections.singletonList(new AskDataColumn("id", "bigint")))),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        AskDataSqlValidationResult result = validator.validate(
                "with scoped as (select id from orders) select scoped.secret from orders scoped",
                mysql, 100, metadata);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("未授权列"));
    }

    @Test
    public void shouldRejectSchemaQualifiedTableWhenMetadataAllowsOnlySimpleTableName() throws Exception {
        AskDataMetadata metadata = new AskDataMetadata("mysql",
                Collections.singletonList(tableAuthorizedAtTableLevel("orders")),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Method method = DefaultAskDataSqlValidator.class.getMethod("validate", String.class, AskDataDialect.class,
                Integer.TYPE, AskDataMetadata.class);

        AskDataSqlValidationResult result = (AskDataSqlValidationResult) method.invoke(validator,
                "select * from other_schema.orders", mysql, Integer.valueOf(100), metadata);

        assertFalse(result.isValid());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
    }

    @Test
    public void shouldRequireExactSchemaQualifiedMetadataTableMatch() throws Exception {
        AskDataMetadata metadata = new AskDataMetadata("mysql",
                Collections.singletonList(tableAuthorizedAtTableLevel("reporting.orders")),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Method method = DefaultAskDataSqlValidator.class.getMethod("validate", String.class, AskDataDialect.class,
                Integer.TYPE, AskDataMetadata.class);

        AskDataSqlValidationResult allowed = (AskDataSqlValidationResult) method.invoke(validator,
                "select * from reporting.orders", mysql, Integer.valueOf(100), metadata);
        AskDataSqlValidationResult otherSchema = (AskDataSqlValidationResult) method.invoke(validator,
                "select * from other_schema.orders", mysql, Integer.valueOf(100), metadata);
        AskDataSqlValidationResult unqualified = (AskDataSqlValidationResult) method.invoke(validator,
                "select * from orders", mysql, Integer.valueOf(100), metadata);

        assertTrue(allowed.isValid());
        assertFalse(otherSchema.isValid());
        assertFalse(unqualified.isValid());
    }

    @Test
    public void shouldAllowMetadataScopedCteAndJoin() throws Exception {
        AskDataMetadata metadata = new AskDataMetadata("mysql", Arrays.asList(
                tableAuthorizedAtTableLevel("orders"),
                tableAuthorizedAtTableLevel("customers")),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Method method = DefaultAskDataSqlValidator.class.getMethod("validate", String.class, AskDataDialect.class,
                Integer.TYPE, AskDataMetadata.class);

        AskDataSqlValidationResult result = (AskDataSqlValidationResult) method.invoke(validator,
                "with recent as (select * from orders) select * from recent join customers on recent.id = customers.id",
                mysql, Integer.valueOf(100), metadata);

        assertTrue(result.isValid());
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
    public void shouldTightenExistingLimitAboveMaximum() {
        AskDataSqlValidationResult result = validator.validate("select * from orders limit 1000000", mysql, 100);

        assertTrue(result.isValid());
        assertEquals("select * from orders limit 100", result.getSql());
    }

    @Test
    public void shouldRejectLimitAll() {
        assertRejected("select * from orders limit all");
    }

    @Test
    public void shouldHonorDisabledSqlLimitRewriting() {
        AskDataSqlValidationResult result = new DefaultAskDataSqlValidator(false)
                .validate("select * from orders", mysql, 100);

        assertTrue(result.isValid());
        assertEquals("select * from orders", result.getSql());
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
    public void shouldDelegateRowLimitDetectionToDialect() {
        AskDataDialect fetchDialect = new AskDataDialect("fetch", "", "") {
            @Override
            public boolean hasRowLimit(String sql) {
                return sql.toLowerCase().contains("fetch first") && sql.toLowerCase().contains("rows only");
            }

            @Override
            public String applyLimit(String sql, int maxRows) {
                return sql + " fetch first " + maxRows + " rows only";
            }
        };

        AskDataSqlValidationResult existingLimit = validator.validate(
                "select * from orders fetch first 10 rows only", fetchDialect, 100);
        AskDataSqlValidationResult missingLimit = validator.validate("select * from orders", fetchDialect, 100);

        assertTrue(existingLimit.isValid());
        assertEquals("select * from orders fetch first 10 rows only", existingLimit.getSql());
        assertTrue(missingLimit.isValid());
        assertEquals("select * from orders fetch first 100 rows only", missingLimit.getSql());
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

    private AskDataTable tableAuthorizedAtTableLevel(String tableName) {
        return new AskDataTable(tableName, Collections.<AskDataColumn>emptyList(), true);
    }
}
