package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;

public interface AskDataSqlExecutor {

    AskDataSqlExecutionResult execute(AskDataRequest request, String sql);
}
