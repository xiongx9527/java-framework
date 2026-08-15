package com.xiongx9527.fw.plus.ai.model;

public final class AiChatResult {

	private final String text;

	private final String actualModelKey;

	private final String finishReason;

	private final AiModelUsage usage;

	public AiChatResult(String text, String actualModelKey, String finishReason, AiModelUsage usage) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if (isBlank(actualModelKey)) {
			throw new IllegalArgumentException("actualModelKey cannot be blank");
		}
		this.text = text;
		this.actualModelKey = actualModelKey;
		this.finishReason = finishReason;
		this.usage = usage == null ? AiModelUsage.unknown() : usage;
	}

	public String getText() {
		return text;
	}

	public String getActualModelKey() {
		return actualModelKey;
	}

	public String getFinishReason() {
		return finishReason;
	}

	public AiModelUsage getUsage() {
		return usage;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
