package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

public class AskDataSqlValidationResult {

    private final boolean valid;
    private final String sql;
    private final String errorCode;
    private final String message;

    private AskDataSqlValidationResult(boolean valid, String sql, String errorCode, String message) {
        this.valid = valid;
        this.sql = sql;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static AskDataSqlValidationResult accepted(String sql) {
        return new AskDataSqlValidationResult(true, sql, null, null);
    }

    public static AskDataSqlValidationResult rejected(String errorCode, String message) {
        return new AskDataSqlValidationResult(false, null, errorCode, message);
    }

    public boolean isValid() {
        return valid;
    }

    public String getSql() {
        return sql;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
