package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand;

public class AskDataSqlValidationResult {

    private final boolean valid;
    private final String sql;
    private final String errorCode;
    private final String message;
    private final AskDataSqlCommand command;

    private AskDataSqlValidationResult(boolean valid, String sql, String errorCode, String message,
            AskDataSqlCommand command) {
        this.valid = valid;
        this.sql = sql;
        this.errorCode = errorCode;
        this.message = message;
        this.command = command;
    }

    public static AskDataSqlValidationResult accepted(String sql) {
        return new AskDataSqlValidationResult(true, sql, null, null, null);
    }

    public static AskDataSqlValidationResult accepted(AskDataSqlCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command 不能为空");
        }
        return new AskDataSqlValidationResult(true, command.getSql(), null, null, command);
    }

    public static AskDataSqlValidationResult rejected(String errorCode, String message) {
        return new AskDataSqlValidationResult(false, null, errorCode, message, null);
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

    public AskDataSqlCommand getCommand() {
        return command;
    }
}
