package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand;

public interface AskDataSqlExecutor {

    AskDataSqlExecutionResult execute(AskDataRequest request, String sql);

    default AskDataSqlExecutionResult execute(AskDataRequest request, AskDataSqlCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command 不能为空");
        }
        return execute(request, command.getSql());
    }
}
