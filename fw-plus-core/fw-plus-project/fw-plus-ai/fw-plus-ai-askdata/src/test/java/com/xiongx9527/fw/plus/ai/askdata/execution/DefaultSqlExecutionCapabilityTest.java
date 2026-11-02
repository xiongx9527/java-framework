package com.xiongx9527.fw.plus.ai.askdata.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlValidator;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class DefaultSqlExecutionCapabilityTest {

    @Test
    void delegatesToExecutorAndReturnsResult() {
        SqlExecutorPort port = (sql, settings) -> QueryResult.builder()
                .columns(Collections.singletonList(new ColumnMeta("id", "BIGINT", false)))
                .rows(Collections.singletonList(Collections.singletonList(1L)))
                .build();
        SqlExecutionCapability cap = new DefaultSqlExecutionCapability(port);
        QueryResult r = cap.execute(SqlExecInput.builder().sql("SELECT id FROM t").build());
        assertEquals(1, r.rowCount());
        assertEquals("id", r.getColumns().get(0).getName());
    }

    @Test
    void readOnlyRejectsWriteDmlBeforeExecuting() {
        AtomicBoolean called = new AtomicBoolean(false);
        SqlExecutorPort port = (sql, settings) -> {
            called.set(true);
            return QueryResult.builder().build();
        };
        SqlExecutionCapability cap = new DefaultSqlExecutionCapability(port, new SqlValidator());
        assertThrows(SqlExecutionException.class,
                () -> cap.execute(SqlExecInput.builder().sql("INSERT INTO t VALUES (1)").build()));
        assertFalse(called.get());
    }

    @Test
    void missingSqlThrows() {
        SqlExecutorPort port = (sql, settings) -> QueryResult.builder().build();
        SqlExecutionCapability cap = new DefaultSqlExecutionCapability(port);
        assertThrows(SqlExecutionException.class,
                () -> cap.execute(SqlExecInput.builder().sql("  ").build()));
    }
}
