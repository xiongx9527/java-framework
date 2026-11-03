package com.xiongx9527.fw.plus.ai.base.cost;

/**
 * 一次调用的成本：token 用量 + 金额。
 */
public final class CostUsage {

    public static final CostUsage ZERO = new CostUsage(0L, 0L, 0.0, null);

    private final long inputTokens;

    private final long outputTokens;

    private final double amount;

    private final String currency;

    public CostUsage(long inputTokens, long outputTokens, double amount, String currency) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.amount = amount;
        this.currency = currency;
    }

    public long getInputTokens() {
        return inputTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }

    public long getTotalTokens() {
        return inputTokens + outputTokens;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
