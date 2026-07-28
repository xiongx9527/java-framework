package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;

public interface AskDataSqlExecutor {

    AskDataSqlExecutionResult execute(AskDataRequest request, String sql);
}
