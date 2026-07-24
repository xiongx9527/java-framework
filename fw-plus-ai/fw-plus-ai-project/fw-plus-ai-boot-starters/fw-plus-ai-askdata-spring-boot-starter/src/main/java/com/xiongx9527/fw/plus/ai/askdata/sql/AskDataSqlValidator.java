package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;

public interface AskDataSqlValidator {

    AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows);

    default AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows,
            AskDataMetadata metadata) {
        return validate(sql, dialect, maxRows);
    }
}
