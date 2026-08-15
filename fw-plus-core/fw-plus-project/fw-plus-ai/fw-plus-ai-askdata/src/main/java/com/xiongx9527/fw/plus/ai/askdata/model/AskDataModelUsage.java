package com.xiongx9527.fw.plus.ai.askdata.model;

import java.math.BigDecimal;

/** 厂商中立的模型 token 与成本计量。 */
public final class AskDataModelUsage {
	private static final AskDataModelUsage UNKNOWN = new AskDataModelUsage(null, null, null, null);
	private final Long inputTokens;
	private final Long outputTokens;
	private final Long totalTokens;
	private final BigDecimal cost;

	public AskDataModelUsage(long inputTokens, long outputTokens, long totalTokens, BigDecimal cost) {
		this(Long.valueOf(inputTokens), Long.valueOf(outputTokens), Long.valueOf(totalTokens), cost);
	}

	private AskDataModelUsage(Long inputTokens, Long outputTokens, Long totalTokens, BigDecimal cost) {
		if (inputTokens != null && (inputTokens.longValue() < 0L || outputTokens == null
				|| outputTokens.longValue() < 0L || totalTokens == null || totalTokens.longValue() < 0L)) {
			throw new IllegalArgumentException("token usage 不能为负数");
		}
		if (cost != null && cost.signum() < 0) throw new IllegalArgumentException("cost 不能为负数");
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
		this.totalTokens = totalTokens;
		this.cost = cost;
	}

	public static AskDataModelUsage unknown() { return UNKNOWN; }
	public Long getInputTokens() { return inputTokens; }
	public Long getOutputTokens() { return outputTokens; }
	public Long getTotalTokens() { return totalTokens; }
	public BigDecimal getCost() { return cost; }
	public boolean isUnknown() { return inputTokens == null; }
}
