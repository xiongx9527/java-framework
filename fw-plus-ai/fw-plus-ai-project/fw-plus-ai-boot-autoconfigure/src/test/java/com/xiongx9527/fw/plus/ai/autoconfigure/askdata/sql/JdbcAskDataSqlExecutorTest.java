package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlParameter;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    public void shouldBindNamedParametersFromRequestOptions() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        jdbcTemplate.execute("drop table if exists named_parameter_orders");
        jdbcTemplate.execute("create table named_parameter_orders(id bigint, amount decimal(10, 2))");
        jdbcTemplate.update("insert into named_parameter_orders(id, amount) values (?, ?)", 1L, 12.50D);
        jdbcTemplate.update("insert into named_parameter_orders(id, amount) values (?, ?)", 2L, 30.00D);
        JdbcAskDataSqlExecutor executor = new JdbcAskDataSqlExecutor(jdbcTemplate);
        AskDataRequest request = new AskDataRequest();
        request.setOptions(Collections.<String, Object>singletonMap("minAmount", 20.00D));

        AskDataSqlExecutionResult result = executor.execute(request,
                "select id from named_parameter_orders where amount >= :minAmount order by id");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getRows().size());
        assertEquals(Long.valueOf(2L), result.getRows().get(0).get("ID"));
    }

    @Test
    public void shouldBindCollectionNamedParametersFromRequestOptions() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        jdbcTemplate.execute("drop table if exists collection_parameter_orders");
        jdbcTemplate.execute("create table collection_parameter_orders(id bigint, amount decimal(10, 2))");
        jdbcTemplate.update("insert into collection_parameter_orders(id, amount) values (?, ?)", 1L, 12.50D);
        jdbcTemplate.update("insert into collection_parameter_orders(id, amount) values (?, ?)", 2L, 30.00D);
        jdbcTemplate.update("insert into collection_parameter_orders(id, amount) values (?, ?)", 3L, 40.00D);
        JdbcAskDataSqlExecutor executor = new JdbcAskDataSqlExecutor(jdbcTemplate);
        AskDataRequest request = new AskDataRequest();
        request.setOptions(Collections.<String, Object>singletonMap("ids", Arrays.asList(1L, 3L)));

        AskDataSqlExecutionResult result = executor.execute(request,
                "select id from collection_parameter_orders where id in (:ids) order by id");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getRows().size());
        assertEquals(Long.valueOf(1L), result.getRows().get(0).get("ID"));
        assertEquals(Long.valueOf(3L), result.getRows().get(1).get("ID"));
    }

    @Test
    public void shouldBindFinalCommandParametersInsteadOfRequestOptions() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        jdbcTemplate.execute("drop table if exists governed_parameter_orders");
        jdbcTemplate.execute("create table governed_parameter_orders(id bigint, tenant_id varchar(32))");
        jdbcTemplate.update("insert into governed_parameter_orders(id, tenant_id) values (?, ?)", 1L, "tenant-a");
        jdbcTemplate.update("insert into governed_parameter_orders(id, tenant_id) values (?, ?)", 2L, "tenant-b");
        JdbcAskDataSqlExecutor executor = new JdbcAskDataSqlExecutor(jdbcTemplate);
        AskDataRequest request = new AskDataRequest();
        request.setOptions(Collections.<String, Object>singletonMap("tenantId", "tenant-b"));
        AskDataSqlCommand command = new AskDataSqlCommand(new AskDataProjectContext("project-a", "operator-a"),
                "select id from governed_parameter_orders where tenant_id = :tenantId",
                Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-a")),
                Collections.singleton("governed_parameter_orders"));

        AskDataSqlExecutionResult result = executor.execute(request, command);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getRows().size());
        assertEquals(Long.valueOf(1L), result.getRows().get(0).get("ID"));
    }

    @Test
    public void shouldSetPhysicalQueryTimeoutOnPreparedStatement() {
        TrackingJdbcTemplate jdbcTemplate = new TrackingJdbcTemplate(true);
        JdbcAskDataSqlExecutor executor = new JdbcAskDataSqlExecutor(jdbcTemplate, 100, 7, true);

        AskDataSqlExecutionResult result = executor.execute(new AskDataRequest(), "select 1");

        assertTrue(result.isSuccess());
        assertEquals(100, jdbcTemplate.maxRows);
        assertEquals(7, jdbcTemplate.queryTimeoutSeconds);
        assertTrue(jdbcTemplate.executeQueryCalled);
    }

    @Test
    public void shouldRejectWritableConnectionWhenReadOnlyConnectionIsRequired() {
        TrackingJdbcTemplate jdbcTemplate = new TrackingJdbcTemplate(false);
        JdbcAskDataSqlExecutor executor = new JdbcAskDataSqlExecutor(jdbcTemplate, 100, 7, true);

        AskDataSqlExecutionResult result = executor.execute(new AskDataRequest(), "select 1");

        assertFalse(result.isSuccess());
        assertEquals("AskData SQL 执行连接必须为只读连接", result.getErrorMessage());
        assertFalse(jdbcTemplate.executeQueryCalled);
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:askdata;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        return dataSource;
    }

    private static class TrackingJdbcTemplate extends JdbcTemplate {

        private final boolean readOnly;
        private int maxRows;
        private int queryTimeoutSeconds;
        private boolean executeQueryCalled;

        private TrackingJdbcTemplate(boolean readOnly) {
            this.readOnly = readOnly;
        }

        @Override
        public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action)
                throws DataAccessException {
            try {
                return action.doInPreparedStatement(preparedStatement());
            } catch (DataAccessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private PreparedStatement preparedStatement() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { PreparedStatement.class }, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            if ("setMaxRows".equals(name)) {
                                maxRows = ((Integer) args[0]).intValue();
                                return null;
                            }
                            if ("setQueryTimeout".equals(name)) {
                                queryTimeoutSeconds = ((Integer) args[0]).intValue();
                                return null;
                            }
                            if ("getConnection".equals(name)) {
                                return connection();
                            }
                            if ("executeQuery".equals(name)) {
                                executeQueryCalled = true;
                                return resultSet();
                            }
                            return defaultValue(method.getReturnType());
                        }
                    });
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { Connection.class }, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            if ("isReadOnly".equals(method.getName())) {
                                return Boolean.valueOf(readOnly);
                            }
                            return defaultValue(method.getReturnType());
                        }
                    });
        }

        private ResultSet resultSet() {
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { ResultSet.class }, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            if ("getMetaData".equals(method.getName())) {
                                return resultSetMetaData();
                            }
                            if ("next".equals(method.getName())) {
                                return Boolean.FALSE;
                            }
                            return defaultValue(method.getReturnType());
                        }
                    });
        }

        private ResultSetMetaData resultSetMetaData() {
            return (ResultSetMetaData) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { ResultSetMetaData.class }, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            if ("getColumnCount".equals(method.getName())) {
                                return Integer.valueOf(0);
                            }
                            return defaultValue(method.getReturnType());
                        }
                    });
        }

        private Object defaultValue(Class<?> returnType) {
            if (Boolean.TYPE.equals(returnType)) {
                return Boolean.FALSE;
            }
            if (Integer.TYPE.equals(returnType)) {
                return Integer.valueOf(0);
            }
            if (Long.TYPE.equals(returnType)) {
                return Long.valueOf(0L);
            }
            if (Double.TYPE.equals(returnType)) {
                return Double.valueOf(0D);
            }
            if (Float.TYPE.equals(returnType)) {
                return Float.valueOf(0F);
            }
            if (Short.TYPE.equals(returnType)) {
                return Short.valueOf((short) 0);
            }
            if (Byte.TYPE.equals(returnType)) {
                return Byte.valueOf((byte) 0);
            }
            return null;
        }
    }
}
