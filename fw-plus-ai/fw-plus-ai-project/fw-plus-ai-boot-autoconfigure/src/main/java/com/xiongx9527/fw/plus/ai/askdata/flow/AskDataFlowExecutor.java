package com.xiongx9527.fw.plus.ai.askdata.flow;

import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.AskDataResult;

public interface AskDataFlowExecutor {

    AskDataResult execute(AskDataRequest request);
}
