package com.xiongx9527.fw.plus.ai.base.tool;

import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import java.util.Collections;
import java.util.List;

/**
 * 工具调用环的一次结果。
 */
public final class ToolCallingResult {

    private static final LlmUsage ZERO = LlmUsage.ZERO;

    private final String answer;

    private final List<ToolTrace> traces;

    private final LlmUsage usage;

    private final ToolStopReason stopReason;

    public ToolCallingResult(String answer, List<ToolTrace> traces, LlmUsage usage, ToolStopReason stopReason) {
        this.answer = answer;
        this.traces = traces == null ? Collections.<ToolTrace>emptyList()
                : Collections.unmodifiableList(traces);
        this.usage = usage == null ? ZERO : usage;
        this.stopReason = stopReason == null ? ToolStopReason.FINAL : stopReason;
    }

    public String getAnswer() {
        return answer;
    }

    public List<ToolTrace> getTraces() {
        return traces;
    }

    public LlmUsage getUsage() {
        return usage;
    }

    public ToolStopReason getStopReason() {
        return stopReason;
    }
}
