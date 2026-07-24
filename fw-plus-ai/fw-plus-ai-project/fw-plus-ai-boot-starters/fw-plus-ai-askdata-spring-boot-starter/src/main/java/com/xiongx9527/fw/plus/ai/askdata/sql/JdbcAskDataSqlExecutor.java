package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.AskDataColumn;
import com.xiongx9527.fw.plus.ai.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcAskDataSqlExecutor implements AskDataSqlExecutor {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public JdbcAskDataSqlExecutor(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public JdbcAskDataSqlExecutor(final JdbcTemplate jdbcTemplate) {
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
    }

    @Override
    public AskDataSqlExecutionResult execute(AskDataRequest request, String sql) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return AskDataSqlExecutionResult.failure(AskDataErrorCode.SQL_EXECUTION_FAILED, "未配置 JdbcTemplate");
        }
        try {
            return jdbcTemplate.query(sql, new ResultSetExtractor<AskDataSqlExecutionResult>() {
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
                    while (resultSet.next()) {
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
