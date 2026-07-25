package com.xiongx9527.fw.plus.ai.askdata.flow;

public interface AskDataSubAgent extends AskDataAgent {

    AskDataSubAgentResult executeSubTask(AskDataSubAgentRequest request);
}
