package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JdbcAskDataSqlExecutorTest {

    @Test
    public void shouldReturnColumnsTypesRowsAndRowCount() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        jdbcTemplate.execute("create table orders(id bigint, amount decimal(10, 2))");
        jdbcTemplate.update("insert into orders(id, amount) values (?, ?)", 1L, 12.50D);
        jdbcTemplate.update("insert into orders(id, amount) values (?, ?)", 2L, 30.00D);
        JdbcAskDataSqlExecutor executor = new JdbcAskDataSqlExecutor(jdbcTemplate);

        AskDataSqlExecutionResult result = executor.execute(new AskDataRequest(),
                "select id, amount from orders order by id");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getColumns().size());
        assertEquals("ID", result.getColumns().get(0).getName());
        assertEquals("BIGINT", result.getColumns().get(0).getType());
        assertEquals("AMOUNT", result.getColumns().get(1).getName());
        assertEquals("DECIMAL", result.getColumns().get(1).getType());
        assertEquals(2, result.getRows().size());
        assertEquals(Long.valueOf(1L), result.getRows().get(0).get("ID"));
        assertEquals(2, result.getRowCount());
    }

    @Test
    public void shouldPhysicallyLimitRowsToRequestMaximum() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        jdbcTemplate.execute("drop table if exists physical_limit_orders");
        jdbcTemplate.execute("create table physical_limit_orders(id bigint)");
        jdbcTemplate.update("insert into physical_limit_orders(id) values (1), (2), (3)");
        JdbcAskDataSqlExecutor executor = new JdbcAskDataSqlExecutor(jdbcTemplate, 100);
        AskDataRequest request = new AskDataRequest();
        request.setMaxRows(Integer.valueOf(2));

        AskDataSqlExecutionResult result = executor.execute(request,
                "select id from physical_limit_orders order by id limit 1000");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getRows().size());
        assertEquals(2, result.getRowCount());
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:askdata;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        return dataSource;
    }
}
