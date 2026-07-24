package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.AskDataColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AskDataSqlExecutionResult {

    private boolean success;
    private String errorCode;
    private String errorMessage;
    private List<AskDataColumn> columns = new ArrayList<AskDataColumn>();
    private List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    private int rowCount;

    public static AskDataSqlExecutionResult success(List<AskDataColumn> columns, List<Map<String, Object>> rows) {
        AskDataSqlExecutionResult result = new AskDataSqlExecutionResult();
        result.success = true;
        result.columns = columns;
        result.rows = rows;
        result.rowCount = rows.size();
        return result;
    }

    public static AskDataSqlExecutionResult failure(String errorCode, String errorMessage) {
        AskDataSqlExecutionResult result = new AskDataSqlExecutionResult();
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<AskDataColumn> getColumns() {
        return columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rowCount;
    }
}
