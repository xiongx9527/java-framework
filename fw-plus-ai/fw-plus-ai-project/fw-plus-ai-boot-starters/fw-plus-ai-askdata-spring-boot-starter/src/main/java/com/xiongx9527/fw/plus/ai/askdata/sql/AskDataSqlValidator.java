package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialect;

public interface AskDataSqlValidator {

    AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows);
}
