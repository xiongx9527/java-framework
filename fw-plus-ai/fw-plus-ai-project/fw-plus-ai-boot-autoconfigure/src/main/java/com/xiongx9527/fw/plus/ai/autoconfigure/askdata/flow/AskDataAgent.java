package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

public interface AskDataAgent {

    String getName();

    AskDataStepResult execute(AskDataStepContext context);
}
