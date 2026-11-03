package com.xiongx9527.fw.plus.ai.base.model;

/**
 * 一次补全的 token 用量。上游未上报分项时对应字段为 {@code 0}。
 */
public final class LlmUsage {

    /** 全零用量（上游未上报时使用）。 */
    public static final LlmUsage ZERO = new LlmUsage(0L, 0L, 0L);

    private final long inputTokens;

    private final long outputTokens;

    private final long totalTokens;

    public LlmUsage(long inputTokens, long outputTokens, long totalTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
    }

    public long getInputTokens() {
        return inputTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    @Override
    public String toString() {
        return "LlmUsage{input=" + inputTokens + ", output=" + outputTokens + ", total=" + totalTokens + '}';
    }
}
