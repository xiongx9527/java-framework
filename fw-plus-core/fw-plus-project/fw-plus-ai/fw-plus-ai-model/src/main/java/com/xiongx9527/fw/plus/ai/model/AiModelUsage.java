package com.xiongx9527.fw.plus.ai.model;

public final class AiModelUsage {

	private static final AiModelUsage UNKNOWN = new AiModelUsage(-1L, -1L, -1L, true);

	private final long inputTokens;

	private final long outputTokens;

	private final long totalTokens;

	public AiModelUsage(long inputTokens, long outputTokens, long totalTokens) {
		this(inputTokens, outputTokens, totalTokens, false);
	}

	private AiModelUsage(long inputTokens, long outputTokens, long totalTokens, boolean unknown) {
		if (!unknown && (inputTokens < 0L || outputTokens < 0L || totalTokens < 0L)) {
			throw new IllegalArgumentException("token usage cannot be negative");
		}
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
		this.totalTokens = totalTokens;
	}

	public static AiModelUsage unknown() {
		return UNKNOWN;
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

	public boolean isUnknown() {
		return this == UNKNOWN;
	}
}
