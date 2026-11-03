package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * 单次工具调用的轨迹（用于可观测与回放）。
 */
public final class ToolTrace {

    private final String name;

    private final String argumentsSummary;

    private final boolean ok;

    private final long latencyMillis;

    public ToolTrace(String name, String argumentsSummary, boolean ok, long latencyMillis) {
        this.name = name;
        this.argumentsSummary = argumentsSummary;
        this.ok = ok;
        this.latencyMillis = latencyMillis;
    }

    public String getName() {
        return name;
    }

    public String getArgumentsSummary() {
        return argumentsSummary;
    }

    public boolean isOk() {
        return ok;
    }

    public long getLatencyMillis() {
        return latencyMillis;
    }

    /** 截断参数 JSON，避免轨迹把整段 SQL / 文档塞进来。 */
    public static String summarize(String arguments, int maxChars) {
        if (arguments == null) {
            return null;
        }
        int limit = maxChars <= 0 ? 200 : maxChars;
        return arguments.length() <= limit ? arguments : arguments.substring(0, limit) + "…";
    }
}
