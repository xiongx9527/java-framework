package com.xiongx9527.fw.plus.ai.askdata.flow;

public interface AskDataAgent {

    String getName();

    AskDataStepResult execute(AskDataStepContext context);
}
