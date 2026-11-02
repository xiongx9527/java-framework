package com.xiongx9527.fw.plus.ai.askdata.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JdbcSqlExecutorTest {

    private DataSource ds;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        this.ds = h2;
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP TABLE IF EXISTS orders");
            st.execute("CREATE TABLE orders (id BIGINT, amount DOUBLE)");
            st.execute("INSERT INTO orders VALUES (1, 10.5), (2, 20.0), (3, 5.0)");
        }
    }

    @Test
    void executesSelectAndMapsColumnsAndRows() {
        JdbcSqlExecutor exec = new JdbcSqlExecutor(ds);
        QueryResult r = exec.execute("SELECT id, amount FROM orders ORDER BY id",
                ExecutionSettings.builder().build());
        assertEquals(2, r.columnCount());
        assertEquals("ID", r.getColumns().get(0).getName());
        assertEquals(3, r.rowCount());
        assertEquals(1L, ((Number) r.getRows().get(0).get(0)).longValue());
        assertEquals(10.5, ((Number) r.getRows().get(0).get(1)).doubleValue(), 0.0001);
    }

    @Test
    void respectsMaxRows() {
        JdbcSqlExecutor exec = new JdbcSqlExecutor(ds);
        QueryResult r = exec.execute("SELECT id FROM orders ORDER BY id",
                ExecutionSettings.builder().maxRows(2).build());
        assertEquals(2, r.rowCount());
        assertTrue(r.isTruncated());
    }

    @Test
    void wrapsSqlExceptionAsBizException() {
        JdbcSqlExecutor exec = new JdbcSqlExecutor(ds);
        SqlExecutionException ex = assertThrows(SqlExecutionException.class,
                () -> exec.execute("SELECT * FROM no_such_table", ExecutionSettings.builder().build()));
        assertTrue(ex.getCause() instanceof SQLException);
    }
}
