package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataColumn;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcAskDataSqlExecutor implements AskDataSqlExecutor {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final int defaultMaxRows;

    public JdbcAskDataSqlExecutor(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this(jdbcTemplateProvider, 100);
    }

    public JdbcAskDataSqlExecutor(ObjectProvider<JdbcTemplate> jdbcTemplateProvider, int defaultMaxRows) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.defaultMaxRows = defaultMaxRows > 0 ? defaultMaxRows : 100;
    }

    public JdbcAskDataSqlExecutor(final JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, 100);
    }

    public JdbcAskDataSqlExecutor(final JdbcTemplate jdbcTemplate, int defaultMaxRows) {
        this.jdbcTemplateProvider = new ObjectProvider<JdbcTemplate>() {
            @Override
            public JdbcTemplate getObject(Object... args) {
                return jdbcTemplate;
            }

            @Override
            public JdbcTemplate getIfAvailable() {
                return jdbcTemplate;
            }

            @Override
            public JdbcTemplate getIfUnique() {
                return jdbcTemplate;
            }

            @Override
            public JdbcTemplate getObject() {
                return jdbcTemplate;
            }
        };
        this.defaultMaxRows = defaultMaxRows > 0 ? defaultMaxRows : 100;
    }

    @Override
    public AskDataSqlExecutionResult execute(AskDataRequest request, String sql) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return AskDataSqlExecutionResult.failure(AskDataErrorCode.SQL_EXECUTION_FAILED, "未配置 JdbcTemplate");
        }
        final int maxRows = request != null && request.getMaxRows() != null && request.getMaxRows().intValue() > 0
                ? request.getMaxRows().intValue() : defaultMaxRows;
        try {
            return jdbcTemplate.query(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setMaxRows(maxRows);
                    return statement;
                }
            }, new ResultSetExtractor<AskDataSqlExecutionResult>() {
                @Override
                public AskDataSqlExecutionResult extractData(ResultSet resultSet) throws SQLException {
                    ResultSetMetaData metadata = resultSet.getMetaData();
                    int columnCount = metadata.getColumnCount();
                    List<AskDataColumn> columns = new ArrayList<AskDataColumn>(columnCount);
                    for (int index = 1; index <= columnCount; index++) {
                        AskDataColumn column = new AskDataColumn();
                        column.setName(metadata.getColumnLabel(index));
                        column.setLabel(metadata.getColumnLabel(index));
                        column.setType(metadata.getColumnTypeName(index));
                        columns.add(column);
                    }
                    List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
                    while (rows.size() < maxRows && resultSet.next()) {
                        Map<String, Object> row = new LinkedHashMap<String, Object>();
                        for (int index = 1; index <= columnCount; index++) {
                            row.put(columns.get(index - 1).getName(), resultSet.getObject(index));
                        }
                        rows.add(row);
                    }
                    return AskDataSqlExecutionResult.success(columns, rows);
                }
            });
        } catch (Exception ex) {
            return AskDataSqlExecutionResult.failure(AskDataErrorCode.SQL_EXECUTION_FAILED, ex.getMessage());
        }
    }
}
