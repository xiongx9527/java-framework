package com.xiongx9527.fw.plus.ai.askdata.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AskDataDispatchResult {

    private boolean success = true;
    private final List<AskDataSubAgentResult> results = new ArrayList<AskDataSubAgentResult>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<AskDataSubAgentResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public void addResult(AskDataSubAgentResult result) {
        results.add(result);
    }
}
